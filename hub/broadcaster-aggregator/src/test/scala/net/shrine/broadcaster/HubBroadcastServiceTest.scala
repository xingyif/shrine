package net.shrine.broadcaster

import net.shrine.aggregation.Aggregator
import net.shrine.problem.TestProblem
import net.shrine.protocol.{AuthenticationInfo, BroadcastMessage, Credential, DeleteQueryRequest, ErrorResponse, FailureResult, FailureResult$, NodeId, Result, ShrineResponse, SingleNodeResult, Timeout}
import net.shrine.util.ShouldMatchersForJUnit
import org.junit.Test

import scala.concurrent.Await

/**
 * @author clint
 * @since Nov 19, 2013
 */
final class HubBroadcastAndAggregationServiceTest extends AbstractSquerylHubDaoTest with ShouldMatchersForJUnit {
  import MockBroadcasters._

  import scala.concurrent.duration._

  private def result(description: Char) = Result(NodeId(description.toString), 1.second, ErrorResponse(TestProblem(summary = "blah blah blah")))

  private val results = "abcde".map(result)

  private lazy val nullResultsByOrigin: Map[NodeId, SingleNodeResult] = Map(NodeId("X") -> null, NodeId("Y") -> null)
  
  private lazy val resultsWithNullsByOrigin: Map[NodeId, SingleNodeResult] = {
    results.collect { case r @ Result(origin, _, _) => origin -> r }.toMap ++ nullResultsByOrigin
  }

  private val broadcastMessage = {
    val authn = AuthenticationInfo("domain", "username", Credential("asdasd", false))

    import scala.concurrent.duration._

    BroadcastMessage(authn, DeleteQueryRequest("projectId", 12345.milliseconds, authn, 12345L))
  }

  @Test
  def testAggregateHandlesNullResults {

    val mockBroadcaster = MockAdapterClientBroadcaster(resultsWithNullsByOrigin)

    val broadcastService = new HubBroadcastAndAggregationService(InJvmBroadcasterClient(mockBroadcaster))

    val aggregator: Aggregator = new Aggregator {
      override def aggregate(results: Iterable[SingleNodeResult], errors: Iterable[ErrorResponse]): ShrineResponse = {
        ErrorResponse(TestProblem(summary = results.size.toString))
      }
    }

    val aggregatedResult = Await.result(broadcastService.sendAndAggregate(broadcastMessage, aggregator, true), 5.minutes)

    mockBroadcaster.messageParam.signature.isDefined should be(false)
    
    aggregatedResult should equal(ErrorResponse(TestProblem(summary = results.size.toString)))
  }

  @Test
  def testAggregateHandlesFailures {
    def toResult(description: Char) = Result(NodeId(description.toString), 1.second, ErrorResponse(TestProblem(summary = "blah blah blah")))

    def toFailure(description: Char) = FailureResult(NodeId(description.toString), new Exception with scala.util.control.NoStackTrace)

    val failuresByOrigin: Map[NodeId, SingleNodeResult] = {
      "UV".map(toFailure).map { case f @ FailureResult(origin, _) => origin -> f }.toMap
    }
    
    val timeoutsByOrigin: Map[NodeId, SingleNodeResult] = Map(NodeId("Z") -> Timeout(NodeId("Z")))
    
    val resultsWithFailuresByOrigin: Map[NodeId, SingleNodeResult] = resultsWithNullsByOrigin ++ failuresByOrigin ++ timeoutsByOrigin

    val mockBroadcaster = MockAdapterClientBroadcaster(resultsWithFailuresByOrigin)

    val broadcastService = new HubBroadcastAndAggregationService(InJvmBroadcasterClient(mockBroadcaster))

    val aggregator: Aggregator = new Aggregator {
      override def aggregate(results: Iterable[SingleNodeResult], errors: Iterable[ErrorResponse]): ShrineResponse = {
        ErrorResponse(TestProblem(summary = s"${results.size},${errors.size}"))
      }
    }

    val aggregatedResult = Await.result(broadcastService.sendAndAggregate(broadcastMessage, aggregator, true), 5.minutes)

    mockBroadcaster.messageParam.signature.isDefined should be(false)
    
    aggregatedResult should equal(ErrorResponse(TestProblem(summary = s"${results.size + failuresByOrigin.size + timeoutsByOrigin.size},0")))
  }
}
