package net.shrine.broadcaster

import scala.concurrent.Await
import org.junit.Test
import net.shrine.util.ShouldMatchersForJUnit
import net.shrine.aggregation.Aggregator
import net.shrine.crypto.DefaultSignerVerifier
import net.shrine.crypto.TestKeystore
import net.shrine.protocol.AuthenticationInfo
import net.shrine.protocol.BroadcastMessage
import net.shrine.protocol.Credential
import net.shrine.protocol.DeleteQueryRequest
import net.shrine.protocol.ErrorResponse
import net.shrine.protocol.Failure
import net.shrine.protocol.NodeId
import net.shrine.protocol.Result
import net.shrine.protocol.ShrineResponse
import net.shrine.protocol.SingleNodeResult
import net.shrine.protocol.Timeout
import net.shrine.crypto.SigningCertStrategy
import net.shrine.broadcaster.dao.MockHubDao

/**
 * @author clint
 * @date Nov 19, 2013
 */
final class SigningBroadcastAndAggregationServiceTest extends ShouldMatchersForJUnit {
  import scala.concurrent.duration._
  import MockBroadcasters._

  private def result(description: Char) = Result(NodeId(description.toString), 1.second, ErrorResponse("blah blah blah"))

  private val results = "abcde".map(result)

  private lazy val nullResultsByOrigin: Map[NodeId, SingleNodeResult] = Map(NodeId("X") -> null, NodeId("Y") -> null)
  
  private lazy val resultsWithNullsByOrigin: Map[NodeId, SingleNodeResult] = {
    results.collect { case r @ Result(origin, _, _) => origin -> r }.toMap ++ nullResultsByOrigin
  }
  private lazy val signer = new DefaultSignerVerifier(TestKeystore.certCollection)

  private val broadcastMessage = {
    val authn = AuthenticationInfo("domain", "username", Credential("asdasd", false))

    import scala.concurrent.duration._

    BroadcastMessage(authn, DeleteQueryRequest("projectId", 12345.milliseconds, authn, 12345L))
  }

  @Test
  def testAggregateHandlesNullResults {

    val mockBroadcaster = MockAdapterClientBroadcaster(resultsWithNullsByOrigin)

    val broadcastService = SigningBroadcastAndAggregationService(InJvmBroadcasterClient(mockBroadcaster), signer, SigningCertStrategy.Attach)

    val aggregator: Aggregator = new Aggregator {
      override def aggregate(results: Iterable[SingleNodeResult], errors: Iterable[ErrorResponse]): ShrineResponse = {
        ErrorResponse(results.size.toString)
      }
    }

    val aggregatedResult = Await.result(broadcastService.sendAndAggregate(broadcastMessage, aggregator, true), 5.minutes)

    mockBroadcaster.messageParam.signature.isDefined should be(true)
    
    aggregatedResult should equal(ErrorResponse(s"${results.size}"))
  }

  @Test
  def testAggregateHandlesFailures {
    def toResult(description: Char) = Result(NodeId(description.toString), 1.second, ErrorResponse("blah blah blah"))

    def toFailure(description: Char) = Failure(NodeId(description.toString), new Exception with scala.util.control.NoStackTrace)

    val failuresByOrigin: Map[NodeId, SingleNodeResult] = {
      "UV".map(toFailure).map { case f @ Failure(origin, _) => origin -> f }.toMap
    }
    
    val timeoutsByOrigin: Map[NodeId, SingleNodeResult] = Map(NodeId("Z") -> Timeout(NodeId("Z")))
    
    val resultsWithFailuresByOrigin: Map[NodeId, SingleNodeResult] = resultsWithNullsByOrigin ++ failuresByOrigin ++ timeoutsByOrigin

    val mockBroadcaster = MockAdapterClientBroadcaster(resultsWithFailuresByOrigin)

    val broadcastService = SigningBroadcastAndAggregationService(InJvmBroadcasterClient(mockBroadcaster), signer, SigningCertStrategy.DontAttach)

    val aggregator: Aggregator = new Aggregator {
      override def aggregate(results: Iterable[SingleNodeResult], errors: Iterable[ErrorResponse]): ShrineResponse = {
        ErrorResponse(s"${results.size},${errors.size}")
      }
    }

    val aggregatedResult = Await.result(broadcastService.sendAndAggregate(broadcastMessage, aggregator, true), 5.minutes)

    mockBroadcaster.messageParam.signature.isDefined should be(true)
    
    aggregatedResult should equal(ErrorResponse(s"${results.size + failuresByOrigin.size + timeoutsByOrigin.size},0"))
  }
}
