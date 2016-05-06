package net.shrine.broadcaster.service

import net.shrine.util.ShouldMatchersForJUnit
import org.junit.Test
import net.shrine.broadcaster.{NodeHandle, Broadcaster, Multiplexer}
import net.shrine.protocol.BroadcastMessage
import net.shrine.protocol.SingleNodeResult
import net.shrine.protocol.Result
import net.shrine.protocol.DeleteQueryResponse
import net.shrine.protocol.NodeId
import net.shrine.protocol.Timeout
import net.shrine.protocol.Failure
import scala.concurrent.Future
import net.shrine.protocol.AuthenticationInfo
import net.shrine.protocol.Credential
import net.shrine.protocol.DeleteQueryRequest

/**
 * @author clint
 * @date Jul 23, 2014
 */
final class BroadcasterMultiplexerServiceTest extends ShouldMatchersForJUnit {
  import scala.concurrent.duration._

  private val authn = AuthenticationInfo("d", "u", Credential("p", false))

  private val message = BroadcastMessage(123456L, authn, DeleteQueryRequest("projectId", 1.second, authn, 98765L))

  @Test
  def testBroadcastAndMultiplex: Unit = {
    val expectedResults = Seq(
      Result(NodeId("X"), 1.second, DeleteQueryResponse(12345)),
      Timeout(NodeId("Y")),
      Failure(NodeId("Z"), new Exception with scala.util.control.NoStackTrace))

    val mockBroadcaster = new Broadcaster {
      var messageParam: BroadcastMessage = _

      override def broadcast(message: BroadcastMessage): Multiplexer = {
        messageParam = message

        new Multiplexer {
          override def processResponse(response: SingleNodeResult): Unit = ()

          override def responses: Future[Iterable[SingleNodeResult]] = Future.successful(expectedResults)
        }
      }

      override def destinations: Set[NodeHandle] = ???
    }

    val service = BroadcasterMultiplexerService(mockBroadcaster, 1.second)

    val responses = service.broadcastAndMultiplex(message)

    mockBroadcaster.messageParam should equal(message)

    responses should equal(expectedResults)
  }

  @Test
  def testBroadcastAndMultiplexFailure: Unit = {
    val mockBroadcaster = new Broadcaster {
      override def broadcast(message: BroadcastMessage): Multiplexer = {
        throw new Exception
      }

      override def destinations: Set[NodeHandle] = ???
    }

    val service = BroadcasterMultiplexerService(mockBroadcaster, 1.second)

    intercept[Exception] {
      service.broadcastAndMultiplex(message)
    }
  }

  @Test
  def testBroadcastAndMultiplexMultiplexerFailure: Unit = {
    val mockBroadcaster = new Broadcaster {
      override def broadcast(message: BroadcastMessage): Multiplexer = new Multiplexer {
        override def processResponse(response: SingleNodeResult): Unit = throw new Exception

        override def responses: Future[Iterable[SingleNodeResult]] = throw new Exception
      }

      override def destinations: Set[NodeHandle] = ???
    }

    val service = BroadcasterMultiplexerService(mockBroadcaster, 1.second)

    intercept[Exception] {
      service.broadcastAndMultiplex(message)
    }
  }
}