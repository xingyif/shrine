package net.shrine.adapter

import org.junit.Test
import net.shrine.util.ShouldMatchersForJUnit

import net.shrine.adapter.dao.squeryl.AbstractSquerylAdapterTest
import net.shrine.protocol.AuthenticationInfo
import net.shrine.protocol.BroadcastMessage
import net.shrine.protocol.Credential
import net.shrine.protocol.ErrorResponse
import net.shrine.protocol.ReadQueryDefinitionRequest
import net.shrine.protocol.ReadQueryDefinitionResponse
import net.shrine.protocol.RenameQueryRequest
import net.shrine.protocol.query.QueryDefinition
import net.shrine.protocol.query.Term

/**
 * @author clint
 * @date Nov 28, 2012
 */
final class ReadQueryDefinitionAdapterTest extends AbstractSquerylAdapterTest with AdapterTestHelpers with ShouldMatchersForJUnit {
  private val networkAuthn = AuthenticationInfo("network-domain", "network-user", Credential("skajdhkasjdh", false))
  
  import scala.concurrent.duration._
  
  @Test
  def testProcessRequest = afterCreatingTables {
    val name = "blarg"
    val expr = Term("foo")
    val fooQuery = QueryDefinition(name,expr)

    val adapter = new ReadQueryDefinitionAdapter(dao)
    
    //Should get error for non-existent query
    {
      val ErrorResponse(msg) = adapter.processRequest(BroadcastMessage(123L, networkAuthn, ReadQueryDefinitionRequest("proj", 1.second, authn, queryId)))

      msg should not be(null)
    }

    //Add a query
    dao.insertQuery(localMasterId, queryId, authn, fooQuery, isFlagged = false, hasBeenRun = true, flagMessage = None)

    //sanity check that it's there
    {
      val Some(query) = dao.findQueryByNetworkId(queryId)

      query.networkId should equal(queryId)
    }

    {
      //Should still get error for non-existent query
      val ErrorResponse(msg) = adapter.processRequest(BroadcastMessage(123L, networkAuthn, ReadQueryDefinitionRequest("proj", 1.second, authn, bogusQueryId)))

      msg should not be(null)
    }
    
    {
      //try to read a real query
      val ReadQueryDefinitionResponse(rQueryId, rName, userId, createDate, queryDefinition) = adapter.processRequest(BroadcastMessage(123L, networkAuthn, ReadQueryDefinitionRequest("proj", 1.second, authn, queryId)))

      rQueryId should equal(queryId)
      rName should equal(name)
      userId should equal(authn.username)
      createDate should not be(null) // :(
      queryDefinition should equal(QueryDefinition(name, expr).toI2b2String)
    }
  }
  
  @Test
  def testProcessRequestBadRequest {
    val adapter = new ReadQueryDefinitionAdapter(dao)

    intercept[Exception] {
      adapter.processRequest(BroadcastMessage(123L, networkAuthn, RenameQueryRequest("proj", 1.second, authn, queryId, "foo")))
    }
  }
}