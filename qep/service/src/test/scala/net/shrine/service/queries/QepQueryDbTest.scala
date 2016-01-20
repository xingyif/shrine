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

  @Test
  def testApply() {

    QepConfigSource.configForBlock("shrine.qep.audit.useQepAudit","true",this.getClass.getSimpleName){

      QepQueryDb.db.insertQepQuery(qepQuery)

      val results = QepQueryDb.db.selectAllQepQueries
      results should equal(Seq(qepQuery))
    }
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
