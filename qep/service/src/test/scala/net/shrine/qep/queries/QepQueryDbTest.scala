package net.shrine.qep.queries

import net.shrine.protocol.{DefaultBreakdownResultOutputTypes, QueryResult, ResultOutputType}
import net.shrine.util.ShouldMatchersForJUnit
import org.junit.{After, Before, Test}

/**
  * @author david 
  * @since 1/20/16
  */
class QepQueryDbTest extends ShouldMatchersForJUnit {

  val qepQuery = QepQuery(
    networkId = 1L,
    userName = "ben",
    userDomain = "testDomain",
    queryName = "testQuery",
    expression = "testExpression",
    dateCreated = System.currentTimeMillis(),
    queryXml = "testXML"
  )

  val secondQepQuery = QepQuery(
    networkId = 2L,
    userName = "dave",
    userDomain = "testDomain",
    queryName = "testQuery",
    expression = "testExpression",
    dateCreated = System.currentTimeMillis(),
    queryXml = "testXML"
  )

  val flag = QepQueryFlag(
    networkQueryId = 1L,
    flagged = true,
    flagMessage = "This query is flagged",
    changeDate = System.currentTimeMillis()
  )

  @Test
  def testInsertQepQuery() {

    QepQueryDb.db.insertQepQuery(qepQuery)
    QepQueryDb.db.insertQepQuery(secondQepQuery)

    val results = QepQueryDb.db.selectAllQepQueries
    results should equal(Seq(qepQuery,secondQepQuery))
  }

  @Test
  def testSelectQepQueriesForUser() {

    QepQueryDb.db.insertQepQuery(qepQuery)
    QepQueryDb.db.insertQepQuery(secondQepQuery)

    val results = QepQueryDb.db.selectPreviousQueriesByUserAndDomain("ben","testDomain")
    results should equal(Seq(qepQuery))
  }

  @Test
  def testSelectQueryFlags() {

    val results1 = QepQueryDb.db.selectMostRecentQepQueryFlagsFor(Set(1L,2L))
    results1 should equal(Map.empty)

    QepQueryDb.db.insertQepQueryFlag(flag)

    val results2 = QepQueryDb.db.selectMostRecentQepQueryFlagsFor(Set(1L,2L))
    results2 should equal(Map(1L -> flag))

  }

  val qepResultRowFromExampleCom = QueryResultRow(
    resultId = 10L,
    networkQueryId = 1L,
    instanceId = 100L,
    adapterNode = "example.com",
    resultType = ResultOutputType.PATIENT_COUNT_XML,
    size = 30L,
    startDate = Some(System.currentTimeMillis() - 60),
    endDate = Some(System.currentTimeMillis() - 30),
    status = QueryResult.StatusType.Finished,
    statusMessage = None,
    changeDate = System.currentTimeMillis()
  )

  @Test
  def testInsertQueryResultRow() {

    QepQueryDb.db.insertQepResultRow(qepResultRowFromExampleCom)

    val results = QepQueryDb.db.selectMostRecentQepResultRowsFor(1L)
    results should equal(Seq(qepResultRowFromExampleCom))
  }

  val qepResultRowFromExampleComInThePast = QueryResultRow(
    resultId = 8L,
    networkQueryId = 1L,
    instanceId = 100L,
    adapterNode = "example.com",
    resultType = ResultOutputType.PATIENT_COUNT_XML,
    size = 0L,
    startDate = qepResultRowFromExampleCom.startDate,
    endDate = None,
    status = QueryResult.StatusType.Processing,
    statusMessage = None,
    changeDate = qepResultRowFromExampleCom.changeDate - 40
  )

  val qepResultRowFromGeneralHospital = QueryResultRow(
    resultId = 100L,
    networkQueryId = 1L,
    instanceId = 100L,
    adapterNode = "generalhospital.org",
    resultType = ResultOutputType.PATIENT_COUNT_XML,
    size = 100L,
    startDate = Some(System.currentTimeMillis() - 60),
    endDate = Some(System.currentTimeMillis() - 30),
    status = QueryResult.StatusType.Finished,
    statusMessage = None,
    changeDate = System.currentTimeMillis()
  )


  @Test
  def testGetMostRecentResultRows() {

    QepQueryDb.db.insertQepResultRow(qepResultRowFromExampleComInThePast)
    QepQueryDb.db.insertQepResultRow(qepResultRowFromGeneralHospital)
    QepQueryDb.db.insertQepResultRow(qepResultRowFromExampleCom)

    val results = QepQueryDb.db.selectMostRecentQepResultRowsFor(1L)
    results.to[Set] should equal(Set(qepResultRowFromExampleCom,qepResultRowFromGeneralHospital))
  }

  val maleRow = QepQueryBreakdownResultsRow(
    networkQueryId = 1L,
    resultId = 100L,
    resultType = DefaultBreakdownResultOutputTypes.PATIENT_GENDER_COUNT_XML,
    dataKey = "male",
    value = 388
  )

  val femaleRow = QepQueryBreakdownResultsRow(
    networkQueryId = 1L,
    resultId = 100L,
    resultType = DefaultBreakdownResultOutputTypes.PATIENT_GENDER_COUNT_XML,
    dataKey = "female",
    value = 390
  )

  val unknownRow = QepQueryBreakdownResultsRow(
    networkQueryId = 1L,
    resultId = 100L,
    resultType = DefaultBreakdownResultOutputTypes.PATIENT_GENDER_COUNT_XML,
    dataKey = "unknown",
    value = 4
  )

  @Test
  def testInsertBreakdownRows(): Unit = {
    QepQueryDb.db.insertQueryBreakdown(maleRow)
    QepQueryDb.db.insertQueryBreakdown(femaleRow)
    QepQueryDb.db.insertQueryBreakdown(unknownRow)

    val results = QepQueryDb.db.selectAllBreakdownResultsRows
    results.to[Set] should equal(Set(maleRow,femaleRow,unknownRow))
  }


  @Before
  def beforeEach() = {
    QepQueryDb.db.createTables()
  }

  @After
  def afterEach() = {
    QepQueryDb.db.dropTables()
  }

}
