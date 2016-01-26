package net.shrine.service.queries

import net.shrine.service.QepConfigSource
import net.shrine.service.QepConfigSource
import net.shrine.util.ShouldMatchersForJUnit
import net.shrine.util.ShouldMatchersForJUnit
import org.junit.{After, Before, Test}

/**
  * @author david 
  * @since 1/20/16
  */
class QepQueryDbTest extends ShouldMatchersForJUnit {// with TestWithDatabase {

  val qepQuery = QepQuery(
    networkId = 1L,
    userName = "ben",
    userDomain = "testDomain",
    queryName = "testQuery",
    expression = "testExpression",
    dateCreated = System.currentTimeMillis(),
    hasBeenRun = false,
    flagged = false,
    flagMessage = "",
    queryXml = "testXML"
  )

  val secondQepQuery = QepQuery(
    networkId = 2L,
    userName = "dave",
    userDomain = "testDomain",
    queryName = "testQuery",
    expression = "testExpression",
    dateCreated = System.currentTimeMillis(),
    hasBeenRun = false,
    flagged = false,
    flagMessage = "",
    queryXml = "testXML"
  )

  @Test
  def testInsert() {

    QepQueryDb.db.insertQepQuery(qepQuery)
    QepQueryDb.db.insertQepQuery(secondQepQuery)

    val results = QepQueryDb.db.selectAllQepQueries
    results should equal(Seq(qepQuery,secondQepQuery))
  }

  @Test
  def testSelectForUser() {

    QepQueryDb.db.insertQepQuery(qepQuery)
    QepQueryDb.db.insertQepQuery(secondQepQuery)

    val results = QepQueryDb.db.selectPreviousQueriesByUserAndDomain("ben","testDomain")
    results should equal(Seq(qepQuery))
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
