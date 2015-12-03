package net.shrine.adapter.client

import net.shrine.util.ShouldMatchersForJUnit
import org.junit.Test
import net.shrine.adapter.service.AdapterRequestHandler
import net.shrine.protocol.BroadcastMessage
import net.shrine.protocol.Result
import net.shrine.protocol.NodeId
import net.shrine.protocol.DeleteQueryResponse
import scala.concurrent.Await

/**
 * @author clint
 * @date Jan 7, 2014
 */
final class InJvmAdapterClientTest extends ShouldMatchersForJUnit {
  @Test
  def testQuery {
    import scala.concurrent.duration._
    
    val nodeId = NodeId("some node")
    
    val resp = DeleteQueryResponse(12345L)
    
    object AlwaysWorksMockAdapter extends AdapterRequestHandler {
      override def handleRequest(request: BroadcastMessage): Result = {
        Result(nodeId, 1.second, resp)
      }
    }
    
    {
      val client = new InJvmAdapterClient(AlwaysWorksMockAdapter)
      
      val result = Await.result(client.query(null), Duration.Inf)
      
      result should equal(Result(nodeId, 1.second, resp))
    }
    
    final class FooException extends Exception
    
    object AlwaysFailsMockAdapter extends AdapterRequestHandler {
      override def handleRequest(request: BroadcastMessage): Result = {
        throw new FooException
      }
    }
    
    {
      val client = new InJvmAdapterClient(AlwaysFailsMockAdapter)
      
      intercept[FooException] {
        Await.result(client.query(null), Duration.Inf)
      }
    }
  }
}