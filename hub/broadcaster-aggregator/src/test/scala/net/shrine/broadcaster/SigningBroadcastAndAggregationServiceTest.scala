package net.shrine.broadcaster

import scala.concurrent.Await
import org.junit.Test
import net.shrine.util.ShouldMatchersForJUnit
import net.shrine.aggregation.Aggregator
import net.shrine.crypto.DefaultSignerVerifier
import net.shrine.crypto.NewTestKeyStore
import net.shrine.protocol.{AuthenticationInfo, BroadcastMessage, Credential, DeleteQueryRequest, ErrorResponse, FailureResult, FailureResult$, NodeId, Result, ShrineResponse, SingleNodeResult, Timeout}
import net.shrine.crypto.SigningCertStrategy
import net.shrine.broadcaster.dao.MockHubDao
import net.shrine.crypto2.SignerVerifierAdapter
import net.shrine.problem.TestProblem

/**
 * @author clint
 * @since Nov 19, 2013
 */
final class SigningBroadcastAndAggregationServiceTest extends ShouldMatchersForJUnit {
  import scala.concurrent.duration._
  import MockBroadcasters._

  private def result(description: Char) = {
    val problem: TestProblem = TestProblem(summary = "blah blah blah")
    Result(NodeId(description.toString), 1.second, ErrorResponse(problem))
  }

  private val results = "abcde".map(result)

  private lazy val nullResultsByOrigin: Map[NodeId, SingleNodeResult] = Map(NodeId("X") -> null, NodeId("Y") -> null)
  
  private lazy val resultsWithNullsByOrigin: Map[NodeId, SingleNodeResult] = {
    results.collect { case r @ Result(origin, _, _) => origin -> r }.toMap ++ nullResultsByOrigin
  }
  private lazy val signer = SignerVerifierAdapter(NewTestKeyStore.certCollection)

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
      override def aggregate(results: Iterable[SingleNodeResult], errors: Iterable[ErrorResponse], respondingTo: BroadcastMessage): ShrineResponse = {
        ErrorResponse(TestProblem(results.size.toString))
      }
    }

    val aggregatedResult = Await.result(broadcastService.sendAndAggregate(broadcastMessage, aggregator, true), 5.minutes)

    mockBroadcaster.messageParam.signature.isDefined should be(true)

    val testProblem = TestProblem(s"${results.size}")
    //testProblem.stamp.time = aggregatedResult.

    aggregatedResult should equal(ErrorResponse(TestProblem(s"${results.size}")))
  }

  @Test
  def testAggregateHandlesFailures {
    def toResult(description: Char) = Result(NodeId(description.toString), 1.second, ErrorResponse(TestProblem("blah blah blah")))

    def toFailure(description: Char) = FailureResult(NodeId(description.toString), new Exception with scala.util.control.NoStackTrace)

    val failuresByOrigin: Map[NodeId, SingleNodeResult] = {
      "UV".map(toFailure).map { case f @ FailureResult(origin, _) => origin -> f }.toMap
    }
    
    val timeoutsByOrigin: Map[NodeId, SingleNodeResult] = Map(NodeId("Z") -> Timeout(NodeId("Z")))
    
    val resultsWithFailuresByOrigin: Map[NodeId, SingleNodeResult] = resultsWithNullsByOrigin ++ failuresByOrigin ++ timeoutsByOrigin

    val mockBroadcaster = MockAdapterClientBroadcaster(resultsWithFailuresByOrigin)

    val broadcastService = SigningBroadcastAndAggregationService(InJvmBroadcasterClient(mockBroadcaster), signer, SigningCertStrategy.DontAttach)

    val aggregator: Aggregator = new Aggregator {
      override def aggregate(results: Iterable[SingleNodeResult], errors: Iterable[ErrorResponse], respondingTo: BroadcastMessage): ShrineResponse = {
        ErrorResponse(TestProblem(s"${results.size},${errors.size}"))
      }
    }

    val aggregatedResult = Await.result(broadcastService.sendAndAggregate(broadcastMessage, aggregator, true), 5.minutes)

    mockBroadcaster.messageParam.signature.isDefined should be(true)
    
    aggregatedResult should equal(ErrorResponse(TestProblem(s"${results.size + failuresByOrigin.size + timeoutsByOrigin.size},0")))
  }
}
