package net.shrine.adapter.audit

import net.shrine.source.ConfigSource
import net.shrine.util.ShouldMatchersForJUnit
import org.junit.{After, Before, Test}

/**
 * @author david 
 * @since 8/25/15
 */
class AdapterAuditDbTest extends ShouldMatchersForJUnit {// with TestWithDatabase {

  val resultsSent = ResultSent(-1,-1,"ben's query",System.currentTimeMillis())
  val executionStarted = ExecutionStarted(-1,"ben's query",System.currentTimeMillis())
  val executionCompleted = ExecutionCompleted(-1,-1,"ben's query",System.currentTimeMillis())
  val queryReceived = QueryReceived("example.com","ben",-1,"ben's query",Some("1"),Some("test topic"),System.currentTimeMillis(),System.currentTimeMillis())

  @Test
  def testInsertResultsSent() {

    ConfigSource.configForBlock("shrine.adapter.audit.useQepAudit","true",this.getClass.getSimpleName){

      AdapterAuditDb.db.insertResultSent(resultsSent)

      val results = AdapterAuditDb.db.selectAllResultsSent
      results should equal(Seq(resultsSent))
    }
  }

  @Test
  def testInsertExecutionStarted() {

    ConfigSource.configForBlock("shrine.adapter.audit.useQepAudit","true",this.getClass.getSimpleName){

      AdapterAuditDb.db.insertExecutionStarted(executionStarted)

      val results = AdapterAuditDb.db.selectAllExecutionStarts
      results should equal(Seq(executionStarted))
    }
  }

  @Test
  def testInsertExecutionCompleted() {

    ConfigSource.configForBlock("shrine.adapter.audit.useQepAudit","true",this.getClass.getSimpleName){

      AdapterAuditDb.db.insertExecutionCompleted(executionCompleted)

      val results = AdapterAuditDb.db.selectAllExecutionCompletes
      results should equal(Seq(executionCompleted))
    }
  }

  @Test
  def testInsertQueryReceived() {

    ConfigSource.configForBlock("shrine.adapter.audit.useQepAudit","true",this.getClass.getSimpleName){

      AdapterAuditDb.db.insertQueryReceived(queryReceived)

      val results = AdapterAuditDb.db.selectAllQueriesReceived
      results should equal(Seq(queryReceived))
    }
  }

  @Before
  def beforeEach() = {
    AdapterAuditDb.db.createTables()
  }

  @After
  def afterEach() = {
    AdapterAuditDb.db.dropTables()
  }

}
