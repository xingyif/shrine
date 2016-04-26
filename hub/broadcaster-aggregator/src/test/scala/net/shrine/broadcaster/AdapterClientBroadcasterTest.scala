package net.shrine.broadcaster

import java.net.URL

import net.shrine.util.ShouldMatchersForJUnit
import org.junit.Test
import net.shrine.protocol.Result
import net.shrine.protocol.NodeId
import net.shrine.protocol.DeleteQueryResponse
import net.shrine.protocol.BroadcastMessage
import net.shrine.protocol.AuthenticationInfo
import net.shrine.protocol.Credential
import net.shrine.protocol.DeleteQueryRequest
import net.shrine.adapter.client.AdapterClient
import net.shrine.protocol.Failure
import scala.concurrent.Future
import scala.concurrent.blocking
import scala.concurrent.Await
import net.shrine.protocol.SingleNodeResult
import net.shrine.client.TimeoutException
import net.shrine.protocol.Timeout
import net.shrine.broadcaster.dao.MockHubDao

/**
 * @author clint
 * @date Dec 3, 2013
 */
final class AdapterClientBroadcasterTest extends ShouldMatchersForJUnit {
  
  private val networkAuthn = AuthenticationInfo("networkd", "networku", Credential("networkp", false))
      
  private val authn = AuthenticationInfo("locald", "localu", Credential("localp", false))
      
  import scala.concurrent.duration._
  
  private val req = DeleteQueryRequest("project-id", 1.second, authn, 12345L)

  private def result(nodeId: NodeId, i: Int) = Result(nodeId, 1.second, DeleteQueryResponse(i))
  
  @Test
  def testCallAdapterTimout {
    final class TimesOutAdapterClient(nodeId: NodeId) extends AdapterClient {
      import scala.concurrent.ExecutionContext.Implicits.global
      
      override def query(message: BroadcastMessage): Future[Result] = Future {
        throw new TimeoutException("foo")
      }
      override def url: Option[URL] = ???

    }
    
    val nodeId1 = NodeId("1") 
    val nodeId2 = NodeId("2")
    
    val dest1 = NodeHandle(nodeId1, new TimesOutAdapterClient(nodeId1))
    
    val expectedResult = result(nodeId2, 2)
    
    val dest2 = NodeHandle(nodeId2, MockAdapterClient(expectedResult))
    
    val broadcaster = AdapterClientBroadcaster(Set(dest1, dest2), MockHubDao)
    
    val timeOutResult = Await.result(broadcaster.callAdapter(BroadcastMessage(networkAuthn, req), dest1), 1.hour)
    
    timeOutResult should equal(Timeout(nodeId1))
    
    val validResult = Await.result(broadcaster.callAdapter(BroadcastMessage(networkAuthn, req), dest2), 1.hour)
    
    validResult should equal(expectedResult)
  }
  
  @Test 
  def testBroadcast {
    import scala.concurrent.duration._
    
    val destinations: Set[NodeHandle] = (for {
      i <- 1 to 5
    } yield {
      val nodeId = NodeId(i.toString)
      
      NodeHandle(nodeId, MockAdapterClient(result(nodeId, i)))
    }).toSet
    
    val broadcaster = AdapterClientBroadcaster(destinations, MockHubDao)
    
    val multiplexer = broadcaster.broadcast(BroadcastMessage(networkAuthn, req))
    
    val responses = Await.result(multiplexer.responses,10.seconds).toSet //Force evaluation of maybe-lazy iterable
    
    val expectedResults = destinations.map(_.client.asInstanceOf[MockAdapterClient].toReturn)
    
    responses should equal(expectedResults)
  }
  
  @Test 
  def testBroadcastSomeFailures {
    import scala.concurrent.duration._
    
    val destinations: Set[NodeHandle] = (for {
      i <- 1 to 3
    } yield {
      val nodeId = NodeId(i.toString)
      
      NodeHandle(nodeId, MockAdapterClient(Result(nodeId, 1.second, DeleteQueryResponse(i))))
    }).toSet
    
    val failingDestinations: Set[NodeHandle] = (for {
      i <- 4 to 5
    } yield {
      val nodeId = NodeId(i.toString)
      
      NodeHandle(nodeId, new AdapterClient {
        import scala.concurrent.ExecutionContext.Implicits.global
        
        override def query(message: BroadcastMessage): Future[Result] = Future { throw new Exception("blarg") }
        override def url: Option[URL] = ???
      })
    }).toSet
    
    val broadcaster = AdapterClientBroadcaster(destinations ++ failingDestinations, MockHubDao)
    
    val multiplexer = broadcaster.broadcast(BroadcastMessage(networkAuthn, req))
    
    val responses = Await.result(multiplexer.responses,10.seconds).toSet //Force evaluation of maybe-lazy iterable
    
    val expectedResults = destinations.map(_.client.asInstanceOf[MockAdapterClient].toReturn)
    
    expectedResults.forall(responses.contains) should be(true)
    
    responses.collect { case Failure(origin, _) => origin } should be(failingDestinations.map(_.nodeId))
  }
}