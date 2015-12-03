package net.shrine.adapter

import net.shrine.adapter.dao.squeryl.AbstractSquerylAdapterTest
import net.shrine.util.ShouldMatchersForJUnit
import net.shrine.protocol.AuthenticationInfo
import net.shrine.protocol.Credential
import org.junit.Test
import net.shrine.protocol.BroadcastMessage
import net.shrine.protocol.FlagQueryRequest
import net.shrine.protocol.FlagQueryResponse
import net.shrine.protocol.query.{QueryDefinition, Term}
import net.shrine.protocol.DeleteQueryRequest

/**
 * @author clint
 */
final class FlagQueryAdapterTest extends AbstractSquerylAdapterTest with AdapterTestHelpers with ShouldMatchersForJUnit {
  private val networkAuthn = AuthenticationInfo("d", "u", Credential("p", false))

  import scala.concurrent.duration._

  @Test
  def testProcessRequest = afterCreatingTables {
    val adapter = new FlagQueryAdapter(dao)

    val flagMessage = "asdasd;alskdlafhksdfh"
    
    val req = FlagQueryRequest("proj", 1.second, authn, queryId, None)
    val reqWithMessage = req.copy(message = Some(flagMessage))
      
    val message = BroadcastMessage(123L, networkAuthn, req)

    //No queries in the DB, should still "work"
    {
      val resp = adapter.processRequest(message)

      resp should equal(FlagQueryResponse)
    }

    //add a query to the db
    val fooQuery = QueryDefinition("some-query",Term("foo"))
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
      //try to flag the query
      val resp = adapter.processRequest(message)

      resp should equal(FlagQueryResponse)

      //Query in the DB should be flagged
      val Some(query) = dao.findQueryByNetworkId(queryId)

      query.networkId should equal(queryId)
      query.isFlagged should be(true)
      query.hasBeenRun should be(true)
      query.flagMessage should be(None)
    }
    
    //Flag it again, this time with a message
    {
      val resp = adapter.processRequest(message.copy(request = reqWithMessage))
      
      resp should equal(FlagQueryResponse)

      //Query in the DB should be flagged
      val Some(query) = dao.findQueryByNetworkId(queryId)

      query.networkId should equal(queryId)
      query.isFlagged should be(true)
      query.hasBeenRun should be(true)
      query.flagMessage should be(Some(flagMessage))
    }
  }

  @Test
  def testProcessRequestBadRequest {
    val adapter = new FlagQueryAdapter(dao)

    intercept[Exception] {
      adapter.processRequest(BroadcastMessage(123L, networkAuthn, DeleteQueryRequest("proj", 1.second, authn, queryId)))
    }
  }
}