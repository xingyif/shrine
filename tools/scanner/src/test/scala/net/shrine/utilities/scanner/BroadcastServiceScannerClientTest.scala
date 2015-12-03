package net.shrine.utilities.scanner

import net.shrine.util.ShouldMatchersForJUnit
import org.junit.Test
import net.shrine.protocol.AuthenticationInfo
import net.shrine.protocol.Credential
import net.shrine.broadcaster.BroadcastAndAggregationService
import net.shrine.protocol.BroadcastMessage
import net.shrine.aggregation.Aggregator
import scala.concurrent.Future
import net.shrine.protocol.ShrineResponse
import net.shrine.authentication.Authenticator
import net.shrine.authentication.AuthenticationResult
import net.shrine.protocol.DeleteQueryResponse
import net.shrine.protocol.AggregatedRunQueryResponse
import net.shrine.util.XmlDateHelper
import net.shrine.protocol.query.Term
import net.shrine.protocol.query.QueryDefinition
import net.shrine.protocol.QueryResult
import net.shrine.protocol.ResultOutputType
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import net.shrine.authentication.NotAuthenticatedException
import net.shrine.protocol.AggregatedReadQueryResultResponse

/**
 * @author clint
 * @date Jan 14, 2014
 */
final class BroadcastServiceScannerClientTest extends ShouldMatchersForJUnit {
  private val authn = AuthenticationInfo("d", "u", Credential("p", false))

  private val projectId = "SHRINE-PROJECT"

  import BroadcastServiceScannerClientTest._
  import scala.concurrent.ExecutionContext.Implicits.{ global => executionContext }

  @Test
  def testQuery {
    val broadcastAndAggregationService = new MockQueryBroadcastAndAggregationService

    val term = "t"

    {
      val clientThatWorks: BroadcastServiceScannerClient = new BroadcastServiceScannerClient(projectId, authn, broadcastAndAggregationService, Authenticators.alwaysWorks, executionContext) {}

      val result = Await.result(clientThatWorks.query(term), Duration.Inf)

      result.count should equal(setSize)
      result.networkQueryId should equal(masterId)
      result.networkResultId should equal(resultId)
      result.status should equal(QueryResult.StatusType.Finished)
      result.term should equal(term)
    }

    {
      val clientThatDoesntAuthenticate: BroadcastServiceScannerClient = new BroadcastServiceScannerClient(projectId, authn, broadcastAndAggregationService, Authenticators.neverWorks, executionContext) {}

      intercept[NotAuthenticatedException] {
        clientThatDoesntAuthenticate.query(term)
      }
    }
  }

  @Test
  def testRetrieveResults {
    val broadcastAndAggregationService = new MockRetrieveResultsBroadcastAndAggregationService

    val term = "t"

    val termResult = TermResult(masterId, resultId, term, QueryResult.StatusType.Processing, setSize)

    {
      val clientThatWorks: BroadcastServiceScannerClient = new BroadcastServiceScannerClient(projectId, authn, broadcastAndAggregationService, Authenticators.alwaysWorks, executionContext) {}

      val result = Await.result(clientThatWorks.retrieveResults(termResult), Duration.Inf)

      result.count should equal(setSize)
      result.networkQueryId should equal(masterId)
      result.networkResultId should equal(resultId)
      result.status should equal(QueryResult.StatusType.Finished)
      result.term should equal(term)
    }

    {
      val clientThatDoesntAuthenticate: BroadcastServiceScannerClient = new BroadcastServiceScannerClient(projectId, authn, broadcastAndAggregationService, Authenticators.neverWorks, executionContext) {}

      intercept[NotAuthenticatedException] {
        clientThatDoesntAuthenticate.retrieveResults(termResult)
      }
    }
  }
}

object BroadcastServiceScannerClientTest {
  private val masterId = 12345L
  private val instanceId = 98765L
  private val resultId = 378256L
  private val setSize = 123L

  private final class MockQueryBroadcastAndAggregationService extends BroadcastAndAggregationService {
    override def sendAndAggregate(message: BroadcastMessage, aggregator: Aggregator, shouldBroadcast: Boolean): Future[ShrineResponse] = Future.successful {
      AggregatedRunQueryResponse(
        masterId,
        XmlDateHelper.now,
        "u",
        "d",
        QueryDefinition("foo", Term("t")),
        instanceId,
        Seq(
          QueryResult(
            resultId,
            instanceId,
            Some(ResultOutputType.PATIENT_COUNT_XML),
            setSize,
            Some(XmlDateHelper.now),
            Some(XmlDateHelper.now),
            None,
            QueryResult.StatusType.Finished,
            None)))
    }
  }

  private final class MockRetrieveResultsBroadcastAndAggregationService extends BroadcastAndAggregationService {
    override def sendAndAggregate(message: BroadcastMessage, aggregator: Aggregator, shouldBroadcast: Boolean): Future[ShrineResponse] = Future.successful {
      AggregatedReadQueryResultResponse(
        masterId,
        Seq(
          QueryResult(
            resultId,
            instanceId,
            Some(ResultOutputType.PATIENT_COUNT_XML),
            setSize,
            Some(XmlDateHelper.now),
            Some(XmlDateHelper.now),
            None,
            QueryResult.StatusType.Finished,
            None)))
    }
  }
}