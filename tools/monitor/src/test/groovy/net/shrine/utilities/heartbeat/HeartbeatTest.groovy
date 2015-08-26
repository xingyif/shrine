package net.shrine.utilities.heartbeat

import groovy.sql.GroovyRowResult
import groovy.sql.Sql
import org.junit.After
import org.junit.Before
import org.junit.Test
import static junit.framework.Assert.*

/**
 * @author Bill Simons
 * @date 1/19/11
 * @link http://cbmi.med.harvard.edu
 * @link http://chip.org
 *       <p/>
 *       NOTICE: This software comes with NO guarantees whatsoever and is
 *       licensed as Lgpl Open Source
 * @link http://www.gnu.org/licenses/lgpl.html
 */
class HeartbeatTest {
  Sql sql
  Heartbeat heartbeat
  List nodes = ['FINISHED_NODE', 'ERROR_NODE', 'PROCESSING_NODE', 'NOT_RESPONDING_NODE']

  @Before
  void setUp() {
    setUpSql()
    heartbeat = new Heartbeat([expectedNumberOfNodes: 3, failureThreshold: [businessHours: 2, offHours: 2], period: [hours: 2]], sql)
  }

  def setUpSql() {
    sql = Sql.newInstance("jdbc:h2:mem:test", "sa", "", "org.h2.Driver")
    sql.execute('''create table NODE (
        ID integer not null auto_increment primary key,
        NAME varchar(20),
        FAILURE_COUNT integer default 0,
        constraint NODE_NAME_IX unique (NAME)
    )''')

    sql.execute('''create table HEARTBEAT_FAILURE (
        ID integer not null auto_increment primary key,
        FAILURE_TIME timestamp default CURRENT_TIMESTAMP,
        MESSAGE varchar(512)
    )''')

    sql.execute('''create table NODE_FAILURE (
        ID integer not null auto_increment primary key,
        FAILURE_ID integer,
        NODE_ID integer,
        constraint FK_NODE_FAILURE_HEARTBEAT_FAILURE_FAILURE_ID foreign key (FAILURE_ID) references HEARTBEAT_FAILURE (ID),
        constraint FK_NODE_FAILURE_NODE_NODE_ID foreign key (NODE_ID) references NODE (ID)
    )''')

    sql.execute('''create table EMAIL_NOTIFICATION (
        LAST_NOTIFICATION timestamp default CURRENT_TIMESTAMP
    )''')

    def nodeFailureCounts = sql.dataSet('NODE')
    nodes.each {
      nodeFailureCounts.add(name: it)
    }
  }

  @After
  void tearDownTables() {
    sql.execute('drop table NODE')
    sql.execute('drop table HEARTBEAT_FAILURE')
    sql.execute('drop table NODE_FAILURE')
    sql.execute('drop table EMAIL_NOTIFICATION')
  }

  @Test
  void testGenerateReport() {
    InputStream stream = HeartbeatTest.class.getResourceAsStream('/net/shrine/utilities/heartbeat/test_response.xml')

    assertNotNull stream

    def report = heartbeat.generateReport(heartbeat.parseResponse(stream.text))
    assertEquals 3, report.numberOfResults
    assertEquals 3, report.nodeResults.size()

    assertTrue report.nodeResults.contains([name: "FINISHED_NODE", status: "FINISHED", statusMessage: ""])
    assertTrue report.nodeResults.contains([name: "ERROR_NODE", status: "ERROR", statusMessage: "Panel 1 contains no mappable terms"])
    assertTrue report.nodeResults.contains([name: "PROCESSING_NODE", status: "PROCESSING", statusMessage: ""])

    assertEquals 3, report.alertingNodes.size()
    assertTrue report.alertingNodes.contains("ERROR_NODE") //error
    assertTrue report.alertingNodes.contains("PROCESSING_NODE") //processing
    assertTrue report.alertingNodes.contains("NOT_RESPONDING_NODE") //not reporting

    assertEquals 2, report.errorNodes.size()
    assertTrue report.errorNodes.contains("ERROR_NODE") //error
    assertTrue report.errorNodes.contains("PROCESSING_NODE") //processing

    assertEquals 1, report.nonReportingNodes.size()
    assertTrue report.nonReportingNodes.contains("NOT_RESPONDING_NODE")
  }


  @Test
  void testGenerateAlertMessage() {
    InputStream stream = HeartbeatTest.class.getResourceAsStream('/net/shrine/utilities/heartbeat/test_response.xml')

    assertNotNull stream

    def report = [:]
    report.queryCompleted = true
    report.numberOfResults = 3
    report.nodeResults = []
    report.nodeResults << [name: "FINISHED_NODE", status: "FINISHED", statusMessage: "FINISHED"]
    report.nodeResults << [name: "ERROR_NODE", status: "ERROR", statusMessage: "Adaptor Mapping error"]
    report.nodeResults << [name: "PROCESSING_NODE", status: "PROCESSING", statusMessage: "PROCESSING"]
    report.nonReportingNodes = ["NOT_RESPONDING_NODE"]

    String alertMessage = heartbeat.generateAlertMessage(report)
    assertNotNull alertMessage
    assertEquals """\
      3 of 4 nodes reporting:
      FINISHED_NODE reported status FINISHED
      ERROR_NODE reported status ERROR: Adaptor Mapping error
      PROCESSING_NODE reported status PROCESSING

      1 of 4 nodes did not report:
      NOT_RESPONDING_NODE
    """.stripIndent(), alertMessage
  }

  @Test
  public void testUpdateDbNodeFailureCount() {
    heartbeat.updateDbNodeFailureCount nodes

    nodes.each { nodeName ->
      def row = sql.firstRow("select * from NODE where NAME=${nodeName}")
      assertNotNull row
      assertEquals 1, row.failure_count
    }

    def nodeName = nodes[0]
    heartbeat.updateDbNodeFailureCount([nodeName])
    def row = sql.firstRow("select * from NODE where NAME=${nodeName}")
    assertNotNull row
    assertEquals 2, row.failure_count
    sql.eachRow("select * from NODE where NAME != ${nodeName}") {
      assertEquals 0, it.failure_count
    }

    heartbeat.updateDbNodeFailureCount([])
    assertNull sql.firstRow("select * from NODE where FAILURE_COUNT > 0")

    heartbeat.updateDbNodeFailureCount(null)
    assertNull sql.firstRow("select * from NODE where FAILURE_COUNT > 0")
  }

  @Test
  public void testIdentifyNonReportingNodes() {
    InputStream stream = HeartbeatTest.class.getResourceAsStream('/net/shrine/utilities/heartbeat/test_response.xml')
    assertNotNull stream

    List nonReportingNodes = heartbeat.identifyNonReportingNodes(heartbeat.parseResponse(stream.text))
    assertEquals 1, nonReportingNodes.size()
    assertTrue nonReportingNodes.contains("NOT_RESPONDING_NODE")

    nonReportingNodes = heartbeat.identifyNonReportingNodes(null)
    assertEquals 4, nonReportingNodes.size()
  }

  @Test
  public void testIdentifyNonNormalNodes() {
    InputStream stream = HeartbeatTest.class.getResourceAsStream('/net/shrine/utilities/heartbeat/test_response.xml')
    assertNotNull stream

    List nonNormalNodes = heartbeat.identifyErrorNodes(heartbeat.parseResponse(stream.text))
    assertEquals 2, nonNormalNodes.size()
    assertTrue nonNormalNodes.contains("ERROR_NODE")
    assertTrue nonNormalNodes.contains("PROCESSING_NODE")
  }

  @Test
  public void testExpectedNumberOfNodes() {
    assertEquals nodes.size(), heartbeat.expectedNumberOfNodes()
  }

  @Test
  public void testThresholdExceeded() {
    assertFalse heartbeat.thresholdExceeded()
    sql.executeUpdate "update NODE set FAILURE_COUNT = 2 where NAME = 'ERROR_NODE'"
    assertTrue heartbeat.thresholdExceeded()
  }

  @Test
  public void testEmailSentToday() {
    sql.executeUpdate "insert into EMAIL_NOTIFICATION VALUES(CURRENT_TIMESTAMP())"
    assertTrue heartbeat.alreadyEmailedThisPeriod()

    sql.executeUpdate "update EMAIL_NOTIFICATION set LAST_NOTIFICATION= DATEADD('HOUR', -1, CURRENT_TIMESTAMP())"
    assertTrue heartbeat.alreadyEmailedThisPeriod()

    sql.executeUpdate "update EMAIL_NOTIFICATION set LAST_NOTIFICATION= DATEADD('HOUR', -3, CURRENT_TIMESTAMP())"
    assertFalse heartbeat.alreadyEmailedThisPeriod()
  }

  @Test
  public void testAuditAlert() {
    def alertMessage = 'alert message'
    heartbeat.auditAlert alertMessage, nodes
    List<GroovyRowResult> failRows = sql.rows('select * from HEARTBEAT_FAILURE')
    assertEquals 1, failRows.size()
    assertEquals alertMessage, failRows[0].message
    assertNotNull failRows[0].failure_time

    List<GroovyRowResult> failNodeRows = sql.rows("select * from NODE_FAILURE where FAILURE_ID=${failRows[0].id}")
    assertEquals nodes.size(), failNodeRows.size()
  }

  @Test
  public void testInBusinessHours() {
    Calendar testTime = Calendar.instance
    println testTime.time

    testTime[Calendar.DAY_OF_WEEK] = Calendar.SUNDAY
    println testTime.time

    assertFalse(heartbeat.inBusinessHours(testTime))

    testTime[Calendar.DAY_OF_WEEK] = Calendar.MONDAY
    testTime[Calendar.HOUR_OF_DAY] = 10
    println testTime.time
    assertTrue(heartbeat.inBusinessHours(testTime))

    testTime[Calendar.HOUR_OF_DAY] = 13
    assertTrue(heartbeat.inBusinessHours(testTime))

    testTime[Calendar.HOUR_OF_DAY] = 8
    assertFalse(heartbeat.inBusinessHours(testTime))

    testTime[Calendar.HOUR_OF_DAY] = 17
    testTime[Calendar.MINUTE] = 1
    assertFalse(heartbeat.inBusinessHours(testTime))
  }
}
