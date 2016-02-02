package net.shrine.qep.queries

import net.shrine.protocol.{QueryResult, ResultOutputType}
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
    id = 10L,
    localId = "Local Id",
    networkQueryId = 1L,
    adapterNode = "example.com",
    resultType = ResultOutputType.PATIENT_COUNT_XML,
    status = QueryResult.StatusType.Finished,
    timeInI2b2 = 30L,
    changeDate = System.currentTimeMillis()
  )

  @Test
  def testInsertQueryResultRow() {

    QepQueryDb.db.insertQepResultRow(qepResultRowFromExampleCom)

    val results = QepQueryDb.db.selectMostRecentQepResultRowsFor(1L)
    results should equal(Seq(qepResultRowFromExampleCom))
  }

  val qepResultRowFromExampleComInThePast = QueryResultRow(
    id = 10L,
    localId = "Local Id",
    networkQueryId = 1L,
    adapterNode = "example.com",
    resultType = ResultOutputType.PATIENT_COUNT_XML,
    status = QueryResult.StatusType.Processing,
    timeInI2b2 = 30L,
    changeDate = qepResultRowFromExampleCom.changeDate - 5000
  )

  val qepResultRowFromGeneralHospital = QueryResultRow(
    id = 14L,
    localId = "Different Local Id",
    networkQueryId = 1L,
    adapterNode = "generalhospital.com",
    resultType = ResultOutputType.PATIENT_COUNT_XML,
    status = QueryResult.StatusType.Finished,
    timeInI2b2 = 30L,
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


  @Before
  def beforeEach() = {
    QepQueryDb.db.createTables()
  }

  @After
  def afterEach() = {
    QepQueryDb.db.dropTables()
  }

}
