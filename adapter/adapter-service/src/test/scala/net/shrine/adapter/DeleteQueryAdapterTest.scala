package net.shrine.adapter

import net.shrine.util.ShouldMatchersForJUnit
import org.junit.Test
import net.shrine.protocol.BroadcastMessage
import net.shrine.protocol.DeleteQueryRequest
import net.shrine.protocol.AuthenticationInfo
import net.shrine.protocol.Credential
import net.shrine.protocol.ErrorResponse
import net.shrine.protocol.DeleteQueryResponse
import net.shrine.protocol.query.{QueryDefinition, Term}
import net.shrine.protocol.RenameQueryRequest
import net.shrine.adapter.dao.squeryl.AbstractSquerylAdapterTest

/**
 * @author clint
 * @date Nov 27, 2012
 */
final class DeleteQueryAdapterTest extends AbstractSquerylAdapterTest with AdapterTestHelpers with ShouldMatchersForJUnit {
  
  private val networkAuthn = AuthenticationInfo("network-domain", "network-user", Credential("alskdjlkasd", false))
  private val localAuthn = AuthenticationInfo("some-domain", "some-user", Credential("alskdjlkasd", false))
  
  import scala.concurrent.duration._
  
  @Test
  def testProcessRequest = afterCreatingTables {
    
    val adapter = new DeleteQueryAdapter(dao)

    {
      val DeleteQueryResponse(returnedId) = adapter.processRequest(BroadcastMessage(123L, networkAuthn, DeleteQueryRequest("proj", 1.second, localAuthn, queryId)))

      returnedId should equal(queryId)
    }

    //Add a query
    val fooQuery = QueryDefinition("some-query",Term("foo"))
    dao.insertQuery(localMasterId, queryId, authn, fooQuery, isFlagged = false, hasBeenRun = true, flagMessage = None)

    //sanity check that it's there
    {
      val Some(query) = dao.findQueryByNetworkId(queryId)

      query.networkId should equal(queryId)
    }

    {
      //try to delete a bogus query
      val DeleteQueryResponse(returnedId) = adapter.processRequest(BroadcastMessage(123L, networkAuthn, DeleteQueryRequest("proj", 1.second, localAuthn, bogusQueryId)))

      returnedId should equal(bogusQueryId)

      //Query in the DB should be unchanged
      val Some(query) = dao.findQueryByNetworkId(queryId)

      query.networkId should equal(queryId)
    }
    
    {
      //try to delete a real query
      val DeleteQueryResponse(returnedId) = adapter.processRequest(BroadcastMessage(123L, networkAuthn, DeleteQueryRequest("proj", 1.second, localAuthn, queryId)))

      returnedId should equal(queryId)

      //Query in the DB should be gone
      dao.findQueryByNetworkId(queryId) should be(None)
    }
  }
  
  @Test
  def testProcessRequestBadRequest {
    val adapter = new DeleteQueryAdapter(dao)

    intercept[Exception] {
      adapter.processRequest(BroadcastMessage(123L, networkAuthn, RenameQueryRequest("proj", 1.second, localAuthn, queryId, "foo")))
    }
  }
}