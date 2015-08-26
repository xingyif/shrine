package net.shrine.adapter

import org.junit.Test
import net.shrine.util.ShouldMatchersForJUnit

import net.shrine.adapter.dao.squeryl.AbstractSquerylAdapterTest
import net.shrine.protocol.AuthenticationInfo
import net.shrine.protocol.BroadcastMessage
import net.shrine.protocol.Credential
import net.shrine.protocol.DeleteQueryRequest
import net.shrine.protocol.RenameQueryRequest
import net.shrine.protocol.RenameQueryResponse
import net.shrine.protocol.query.{QueryDefinition, Term}

/**
 * @author clint
 * @date Nov 27, 2012
 */
final class RenameQueryAdapterTest extends AbstractSquerylAdapterTest with AdapterTestHelpers with ShouldMatchersForJUnit {
  private val networkAuthn = AuthenticationInfo("network-domain", "network-user", Credential("skajdhkasjdh", false))
  
  import scala.concurrent.duration._
  
  @Test
  def testProcessRequest = afterCreatingTables {
    val name = "blarg"
    val fooQuery = QueryDefinition(name,Term("foo"))

    val newName = "nuh"
    
    val adapter = new RenameQueryAdapter(dao)
    
    //No queries in the DB, should still "work"
    {
      val RenameQueryResponse(returnedId, returnedName) = adapter.processRequest(BroadcastMessage(123L, networkAuthn, RenameQueryRequest("proj", 1.second, authn, queryId, name)))
      
      returnedId should equal(queryId)
      returnedName should equal(name)
    }
    
    //add a query to the db
    dao.insertQuery(localMasterId, queryId, authn, fooQuery, isFlagged = false, hasBeenRun = true, flagMessage = None)

    //sanity check that it's there
    {
      val Some(query) = dao.findQueryByNetworkId(queryId)

      query.networkId should equal(queryId)
    }
    
    {
      //try to rename a bogus query
      val RenameQueryResponse(returnedId, returnedName) = adapter.processRequest(BroadcastMessage(123L, networkAuthn, RenameQueryRequest("proj", 1.second, authn, bogusQueryId, newName)))

      returnedId should equal(bogusQueryId)
      returnedName should equal(newName)

      //Query in the DB should be unchanged
      val Some(query) = dao.findQueryByNetworkId(queryId)

      query.name should equal(name)
    }
    
    {
      //try to rename a real query
      val RenameQueryResponse(returnedId, returnedName) = adapter.processRequest(BroadcastMessage(123L, networkAuthn, RenameQueryRequest("proj", 1.second, authn, queryId, newName)))

      returnedId should equal(queryId)
      returnedName should equal(newName)

      //Query in the DB should be renamed
      val Some(query) = dao.findQueryByNetworkId(queryId)
      
      query.name should equal(newName)
    }
  }
  
  @Test
  def testProcessRequestBadRequest {
    val adapter = new RenameQueryAdapter(dao)

    intercept[Exception] {
      adapter.processRequest(BroadcastMessage(123L, networkAuthn, DeleteQueryRequest("proj", 1.second, authn, queryId)))
    }
  }
}