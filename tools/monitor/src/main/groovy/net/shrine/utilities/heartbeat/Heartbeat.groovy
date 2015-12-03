package net.shrine.utilities.heartbeat

import groovy.sql.Sql
import groovy.util.slurpersupport.GPathResult
import net.shrine.protocol.QueryDefinitionConfig
import net.shrine.protocol.AuthenticationInfo
import net.shrine.protocol.Credential
import net.shrine.protocol.ResultOutputType
import net.shrine.protocol.query.QueryDefinition
import net.shrine.client.JerseyShrineClient
import org.apache.commons.mail.DefaultAuthenticator
import org.apache.commons.mail.Email
import org.apache.commons.mail.SimpleEmail
import org.apache.log4j.Logger

/**
 * @author Bill Simons
 * @date Dec 17, 2010
 * @link http://cbmi.med.harvard.edu
 * @link http://chip.org
 *       <p/>
 *       NOTICE: This software comes with NO guarantees whatsoever and is
 *       licensed as Lgpl Open Source
 * @link http://www.gnu.org/licenses/lgpl.html
 */
class Heartbeat {
  static Logger logger = Logger.getLogger(Heartbeat.class);
  def config
  Sql sql;

  Heartbeat(config, Sql sql) {
    this.config = config
    this.sql = sql
  }

  def run() {
    def alertMessage = ""
    boolean normalExecution = true
    def alertingNodes = []
    try {
      queryDefinitions().each {
        def response = queryShrine(it)

        def report = generateReport(response)
        if(isFailure(report)) {
          normalExecution = false
          alertMessage += generateAlertMessage(report)
          alertMessage += '\n\n'
          alertingNodes.addAll report.alertingNodes
        }
      }
    }
    catch (Exception e) {
      e.printStackTrace()
      println e.response.data
      normalExecution = false
      alertMessage += e.getMessage()
    }

    try {
      updateDbNodeFailureCount alertingNodes
      if(!normalExecution) {
        handleAlert alertMessage, alertingNodes
      }
    }
    catch (Exception e) {
      logger.error("Failed to complete", e)
    }

  }

  boolean shouldAlert() {
    thresholdExceeded() && !alreadyEmailedThisPeriod()
  }

  boolean alreadyEmailedThisPeriod() {
    def row = sql.firstRow('select LAST_NOTIFICATION from EMAIL_NOTIFICATION')
    row.last_notification

    def now = new Date()
    Date lastEmailTime = (row.last_notification) ? row.last_notification : (now - 1)
    lastEmailTime.hours = lastEmailTime.hours + config.period.hours

    !now.after(lastEmailTime)
  }

  def updateLastNotificationTime() {
    sql.executeUpdate "update EMAIL_NOTIFICATION set LAST_NOTIFICATION=CURRENT_TIMESTAMP()"
  }

  boolean thresholdExceeded() {
    def row = sql.firstRow("select 1 from NODE where FAILURE_COUNT >= ${determineThreshold()}")
    row
  }

  int determineThreshold() {
    if(inBusinessHours(Calendar.instance)) {
      config.failureThreshold.businessHours
    } else {
      config.failureThreshold.offHours
    }
  }

  boolean inBusinessHours(Calendar now) {
    def dayOfWeek = now[Calendar.DAY_OF_WEEK]
    def isAWeekday = dayOfWeek != Calendar.SATURDAY && dayOfWeek != Calendar.SUNDAY
    Calendar nine = Calendar.instance
    nine.clearTime()
    nine[Calendar.HOUR_OF_DAY] = 9

    Calendar five = Calendar.instance
    five.clearTime()
    five[Calendar.HOUR_OF_DAY] = 17

    def afterNine = now[Calendar.HOUR_OF_DAY] >= 9
    def beforeFive = now[Calendar.HOUR_OF_DAY] < 17
    isAWeekday && afterNine && beforeFive
  }

  def updateDbNodeFailureCount(alertingNodes) {
    if(alertingNodes == null || alertingNodes.size < 1) {
      sql.executeUpdate "update NODE set FAILURE_COUNT = 0"
    }
    else {
      def nodeNameList = alertingNodes.collect { "'${it}'" }.join(',')
      sql.withTransaction {
        sql.executeUpdate "update NODE set FAILURE_COUNT = FAILURE_COUNT + 1 where NAME in (${nodeNameList})".toString()
        sql.executeUpdate "update NODE set FAILURE_COUNT = 0 where NAME not in (${nodeNameList})".toString()
      }
    }
  }

  boolean isFailure(report) {
    (report.alertingNodes.size() > 0) || (report.nodeResults.size() != expectedNumberOfNodes())
  }

  int expectedNumberOfNodes() {
    def row = sql.firstRow("select count(*) as NODE_NUMBER from NODE")
    row.node_number
  }

  def generateReport(response) {
    def analysis = [:]
    analysis.numberOfResults = determineNumberOfResults(response)
    analysis.nodeResults = []
    response?.queryResults?.queryResult?.each {
      if("AGGREGATED" != it.description.toString()) {
        analysis.nodeResults << [name: it.description.toString(), status: it.status.toString(), statusMessage: it.statusMessage.toString()]
      }
    }

    analysis.nonReportingNodes = identifyNonReportingNodes(response)
    analysis.errorNodes = identifyErrorNodes(response)
    analysis.alertingNodes = []
    analysis.alertingNodes.addAll(analysis.errorNodes)
    analysis.alertingNodes.addAll(analysis.nonReportingNodes)

    analysis
  }

  List identifyErrorNodes(response) {
    def errorNodes = response?.queryResults?.queryResult?.findAll {
      it.description != "AGGREGATED" && it.status != "FINISHED"
    }.collect {
      it.description.toString()
    }

    errorNodes
  }

  int determineNumberOfResults(response) {
    response?.queryResults?.queryResult?.findAll {
      it.description != "AGGREGATED"
    }.size()
  }

  String generateAlertMessage(report) {
    """\
    |${report.numberOfResults} of ${expectedNumberOfNodes()} nodes reporting:
    ${ out ->
      report.nodeResults.each { result ->
        out << "|${result.name} reported status ${result.status}"
        if("ERROR" == result.status && result.statusMessage != null) {
          out << ": ${result.statusMessage}\n"
        }
        else {
          out << "\n"
        }
      }
    }
    |${expectedNumberOfNodes() - report.numberOfResults} of ${expectedNumberOfNodes()} nodes did not report:
    ${ out ->
      report.nonReportingNodes.each { result ->
        out << "|${result}"
        out << "\n"
      }
    }""".stripMargin().toString()
  }

  def parseResponse(String originalResponse) {
    GPathResult responseGPath = new XmlSlurper().parseText(originalResponse)
    obscurePassword(responseGPath)
    responseGPath
  }

  def obscurePassword(GPathResult responseGPath) {
    responseGPath.message_header.security.password = '*****'
  }

  def handleAlert(alertMessage, alertingNodes) {
    logger.warn alertMessage
    auditAlert alertMessage, alertingNodes

    if(shouldAlert()) {
      String subject = "SHRINE system alert - ${alertingNodes?.isEmpty() ? "General" : alertingNodes.join(',')}"
      Email email = new SimpleEmail()
      email.hostName = config.mail.host
      email.smtpPort = config.mail.port
      email.authenticator = new DefaultAuthenticator(config.mail.username, config.mail.password)
      email.TLS = config.mail.usetls
      email.from = config.mail.from
      email.subject = subject
      email.msg = "Network in non-functional state on ${new Date()}:\r\n${alertMessage}"
      email.addTo(config.mail.recipient)
      email.send()

      updateLastNotificationTime()
    }
  }

  def auditAlert(alertMessage, alertingNodes) {
    sql.withTransaction {
      def keys = sql.executeInsert("insert into HEARTBEAT_FAILURE (MESSAGE) values(${alertMessage})")
      def failureId = keys[0][0]
      alertingNodes.each {
        def node = sql.firstRow("select ID from NODE where NAME=${it}")
        sql.executeInsert("insert into NODE_FAILURE (FAILURE_ID, NODE_ID) values(${failureId}, ${node.id})")
      }
    }
  }

  def queryShrine(QueryDefinition queryDefinition) {
    def authInfo = new AuthenticationInfo(config.i2b2.domain, config.i2b2.username, new Credential(config.i2b2.password, false))

	//NB: Groovy doesn't like accessing nested Scala objects, hence the clunky usage here. :(
	def trustParam = net.shrine.crypto.TrustParam$AcceptAllCerts$.MODULE$
	
    def shrineClient = new JerseyShrineClient(config.nodeUrl, config.i2b2.projectId, authInfo, trustParam)

	//NB: Groovy doesn't like accessing Scala objects, hence the clunky usage here
	def outputType = net.shrine.protocol.ResultOutputType$.MODULE$.PATIENT_COUNT_XML

    def outputTypes = [outputType] as Set

    def queryTopicId = config.sheriff.queryTopicId.toString()

    def response = shrineClient.runQuery(queryTopicId,
            outputTypes,
            queryDefinition,
            true)

    //Turn the response into XML parsed by XmlSlurper, to maintain the contract
    //TODO: Avoid this second marshalling round trip
    new XmlSlurper().parseText(response.toXmlString())
  }

  Iterator<QueryDefinition> queryDefinitions() {
    QueryDefinitionConfig.loadQueryDefinitionConfig(config.queryDefinitions.file)
  }

  List identifyNonReportingNodes(response) {
    List reportingNodes = response?.queryResults?.queryResult?.description.findAll {
      it != "AGGREGATED"
    }.collect {"'${it}'"}

    if(reportingNodes.size > 0) {
      return sql.rows("select NAME from NODE where NAME not in (${reportingNodes.join(",")})".toString()).collect {
        it.NAME
      }
    }
    else {
      return sql.rows("select NAME from NODE").collect {
        it.NAME
      }
    }
  }

  public static void main(String[] args) {
    def config = new ConfigSlurper().parse(new File(args[0]).toURL())
    def sql = Sql.newInstance(config.dbUrl, config.dbUser, config.dbPasswd ?: "", config.dbDriver)
    def heartbeat = new Heartbeat(config, sql)
    heartbeat.run()
    println 'Heartbeat finished'
  }
}