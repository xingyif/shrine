package net.shrine.broadcaster.service

import net.shrine.util.ShouldMatchersForJUnit
import org.junit.Test
import net.shrine.protocol.BroadcastMessage
import net.shrine.protocol.SingleNodeResult
import net.shrine.protocol.AuthenticationInfo
import net.shrine.protocol.Credential
import net.shrine.protocol.DeleteQueryRequest
import net.shrine.protocol.NodeId
import net.shrine.protocol.Result
import net.shrine.protocol.Timeout
import net.shrine.protocol.FailureResult$
import net.shrine.protocol.DeleteQueryResponse
import net.shrine.protocol.MultiplexedResults
import net.shrine.protocol.DefaultBreakdownResultOutputTypes
import scala.xml.XML

/**
 * @author clint
 * @date Jul 23, 2014
 */
final class BroadcasterMultiplexerResourceTest extends ShouldMatchersForJUnit {
  @Test
  def testBroadcastAndMultiplexBadInput: Unit = {
    val resource = BroadcasterMultiplexerResource(null)

    {
      val response = resource.broadcastAndMultiplex(null)

      response.getStatus should equal(400)
    }

    {
      val response = resource.broadcastAndMultiplex("aslkjasldj")

      response.getStatus should equal(400)
    }
  }

  @Test
  def testBroadcastAndMultiplexFails: Unit = {
    val resource = BroadcasterMultiplexerResource(new BroadcasterMultiplexerRequestHandler {
      override def broadcastAndMultiplex(message: BroadcastMessage) = throw new Exception
    })

    val authn = AuthenticationInfo("d", "u", Credential("p", false))

    import scala.concurrent.duration._
    
    val message = BroadcastMessage(123456L, authn, DeleteQueryRequest("projectId", 1.second, authn, 98765L))

    val response = resource.broadcastAndMultiplex(message.toXmlString)

    response.getStatus should equal(500)
  }
  
  @Test
  def testBroadcastAndMultiplex: Unit = {
    import scala.concurrent.duration._
    
    val expectedResults = Seq(
      Result(NodeId("X"), 1.second, DeleteQueryResponse(12345)),
      Timeout(NodeId("Y")),
      Timeout(NodeId("Z")))
    
    val resource = BroadcasterMultiplexerResource(new BroadcasterMultiplexerRequestHandler {
      override def broadcastAndMultiplex(message: BroadcastMessage) = expectedResults
    })

    val authn = AuthenticationInfo("d", "u", Credential("p", false))
    
    val message = BroadcastMessage(123456L, authn, DeleteQueryRequest("projectId", 1.second, authn, 98765L))

    val response = resource.broadcastAndMultiplex(message.toXmlString)

    response.getStatus should equal(200)

    MultiplexedResults.fromXml(DefaultBreakdownResultOutputTypes.toSet)(XML.loadString(response.getEntity.asInstanceOf[String])).get.results should equal(expectedResults)
  }
}