package net.shrine.adapter

import net.shrine.util.ShouldMatchersForJUnit
import net.shrine.adapter.dao.squeryl.AbstractSquerylAdapterTest
import net.shrine.protocol.AuthenticationInfo
import org.junit.Test
import net.shrine.protocol.Credential
import net.shrine.protocol.UnFlagQueryRequest
import net.shrine.protocol.BroadcastMessage
import net.shrine.protocol.UnFlagQueryResponse
import net.shrine.protocol.query.{QueryDefinition, Term}
import net.shrine.protocol.DeleteQueryRequest

/**
 * @author clint
 * @date Apr 18, 2014
 */
final class UnFlagQueryAdapterTest extends AbstractSquerylAdapterTest with AdapterTestHelpers with ShouldMatchersForJUnit {
  private val networkAuthn = AuthenticationInfo("d", "u", Credential("p", false))

  import scala.concurrent.duration._

  @Test
  def testProcessRequest = afterCreatingTables {
    val adapter = new UnFlagQueryAdapter(dao)

    val flagMessage = "asdasd;alskdlafhksdfh"
    
    val req = UnFlagQueryRequest("proj", 1.second, authn, queryId)
    
    val message = BroadcastMessage(123L, networkAuthn, req)

    //No queries in the DB, should still "work"
    {
      val resp = adapter.processRequest(message)

      resp should equal(UnFlagQueryResponse)
    }

    //add a query to the db
    val fooQuery = QueryDefinition("some query name",Term("foo"))
    dao.insertQuery(localMasterId, queryId, authn, fooQuery, isFlagged = false, hasBeenRun = true, flagMessage = None)

    //sanity check that it's there
    {
      val Some(query) = dao.findQueryByNetworkId(queryId)

      query.networkId should equal(queryId)
      query.isFlagged should be(false)
      query.hasBeenRun should be(true)
      query.flagMessage should be(None)
    }

    {
      //try to un-flag the query, unflagging an un-flagegd query should still "work"
      val resp = adapter.processRequest(message)

      resp should equal(UnFlagQueryResponse)

      //Query in the DB should still be un-flagged
      val Some(query) = dao.findQueryByNetworkId(queryId)

      query.networkId should equal(queryId)
      query.isFlagged should be(false)
      query.hasBeenRun should be(true)
      query.flagMessage should be(None)
    }
    
    //flag the query so we can un-flag it
    
    dao.flagQuery(req.networkQueryId, Some("askjldhkasd"))
    
    dao.findQueryByNetworkId(queryId).get.isFlagged should be(true)
    
    //Un-flag the query
    {
      val resp = adapter.processRequest(message)
      
      resp should equal(UnFlagQueryResponse)

      //Query in the DB should be un-flagged
      val Some(query) = dao.findQueryByNetworkId(queryId)

      query.networkId should equal(queryId)
      query.isFlagged should be(false)
      query.hasBeenRun should be(true)
      query.flagMessage should be(None)
    }
  }

  @Test
  def testProcessRequestBadRequest {
    val adapter = new UnFlagQueryAdapter(dao)

    intercept[Exception] {
      adapter.processRequest(BroadcastMessage(123L, networkAuthn, DeleteQueryRequest("proj", 1.second, authn, queryId)))
    }
  }
}