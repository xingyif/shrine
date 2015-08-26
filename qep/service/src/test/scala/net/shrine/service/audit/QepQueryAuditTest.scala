package net.shrine.service.audit

import net.shrine.service.QepConfigSource
import net.shrine.util.ShouldMatchersForJUnit
import org.junit.{After, Before, Test}

/**
 * @author david 
 * @since 8/18/15
 */
class QepQueryAuditTest extends ShouldMatchersForJUnit {// with TestWithDatabase {

  val qepAuditData = QepQueryAuditData("example.com","ben",-1,"ben's query",Some("ben's topic"))

  @Test
  def testApply() {

    QepConfigSource.configForBlock("shrine.qep.audit.useQepAudit","true",this.getClass.getSimpleName){

      QepAuditDb.db.insertQepQuery(qepAuditData)

      val results = QepAuditDb.db.selectAllQepQueries
      results should equal(Seq(qepAuditData))
    }
  }

  @Before
  def beforeEach() = {
    QepAuditDb.db.createTables()
  }

  @After
  def afterEach() = {
    QepAuditDb.db.dropTables()
  }

}
