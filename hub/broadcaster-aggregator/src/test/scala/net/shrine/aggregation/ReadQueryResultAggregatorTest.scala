package net.shrine.aggregation

import scala.concurrent.duration.DurationInt
import org.junit.Test
import net.shrine.util.ShouldMatchersForJUnit
import net.shrine.protocol.AggregatedReadQueryResultResponse
import net.shrine.protocol.ErrorResponse
import net.shrine.protocol.NodeId
import net.shrine.protocol.QueryResult
import net.shrine.protocol.ReadQueryResultResponse
import net.shrine.protocol.Result
import net.shrine.protocol.ResultOutputType
import net.shrine.protocol.BaseShrineResponse

/**
 * @author clint
 * @since Nov 7, 2012
 */
final class ReadQueryResultAggregatorTest extends ShouldMatchersForJUnit {
  private val queryId = 12345L

  import ResultOutputType._

  private def asAggregatedResponse(resp: BaseShrineResponse) = resp.asInstanceOf[AggregatedReadQueryResultResponse]

  private val setSize1 = 123L
  private val setSize2 = 456L
  private val totalSetSize = setSize1 + setSize2

  private val queryResult1 = QueryResult(1L, 2L, Some(PATIENT_COUNT_XML), setSize1, None, None, None, QueryResult.StatusType.Finished, None)
  private val queryResult2 = QueryResult(1L, 2L, Some(PATIENT_COUNT_XML), setSize2, None, None, None, QueryResult.StatusType.Finished, None)

  private val response1 = ReadQueryResultResponse(queryId, queryResult1)
  private val response2 = ReadQueryResultResponse(queryId, queryResult2)

  import scala.concurrent.duration._
  
  private val result1 = Result(NodeId("X"), 1.second, response1)
  private val result2 = Result(NodeId("Y"), 1.second, response2)

  private val errors = Seq(ErrorResponse("blarg"), ErrorResponse("glarg"))

  @Test
  def testAggregate {
    val aggregator = new ReadQueryResultAggregator(queryId, true)

    val response = asAggregatedResponse(aggregator.aggregate(Seq(result1, result2), Nil))

    val Seq(actualQueryResult1, actualQueryResult2, aggregatedQueryResult) = response.results

    actualQueryResult1 should equal(queryResult1)
    actualQueryResult2 should equal(queryResult2)

    val expectedAggregatedResult = queryResult1.withSetSize(totalSetSize).withInstanceId(queryId).withDescription("Aggregated Count")

    aggregatedQueryResult should equal(expectedAggregatedResult)
  }

  @Test
  def testAggregateNoAggregatedResult {
    val aggregator = new ReadQueryResultAggregator(queryId, false)

    val response = asAggregatedResponse(aggregator.aggregate(Seq(result1, result2), Nil))

    val Seq(actualQueryResult1, actualQueryResult2) = response.results

    actualQueryResult1 should equal(queryResult1)
    actualQueryResult2 should equal(queryResult2)
  }

  @Test
  def testAggregateNoResponses {
    for (doAggregation <- Seq(true, false)) {
      val aggregator = new ReadQueryResultAggregator(queryId, true)

      val response = asAggregatedResponse(aggregator.aggregate(Nil, Nil))

      response.queryId should equal(queryId)
      response.results.isEmpty should be(true)
    }
  }

  @Test
  def testAggregateOnlyErrorResponses {
    val aggregator = new ReadQueryResultAggregator(queryId, true)

    val response = asAggregatedResponse(aggregator.aggregate(Nil, errors))

    response.queryId should equal(queryId)

    response.results.exists(qr => qr.problemDigest.exists(pd => pd.codec == classOf[ErrorResultProblem].getName)) should be (true)
  }

  @Test
  def testAggregateSomeErrors {
    val aggregator = new ReadQueryResultAggregator(queryId, true)

    val response = asAggregatedResponse(aggregator.aggregate(Seq(result1, result2), errors))

    val Seq(actualQueryResult1, actualQueryResult2, aggregatedQueryResult, actualErrorQueryResults @ _*) = response.results

    actualQueryResult1 should equal(queryResult1)
    actualQueryResult2 should equal(queryResult2)

    val expectedAggregatedResult = queryResult1.withSetSize(totalSetSize).withInstanceId(queryId).withDescription("Aggregated Count")

    aggregatedQueryResult should equal(expectedAggregatedResult)

    actualErrorQueryResults.exists(qr => qr.problemDigest.exists(pd => pd.codec == classOf[ErrorResultProblem].getName)) should be (true)
  }
  
  @Test
  def testAggregateSomeDownstreamErrors {
    val aggregator = new ReadQueryResultAggregator(queryId, true)

    val result3 = Result(NodeId("A"), 1.second, errors.head)
    val result4 = Result(NodeId("A"), 1.second, errors.last)
    
    val response = asAggregatedResponse(aggregator.aggregate(Seq(result1, result2, result3, result4), Nil))

    val Seq(actualQueryResult1, actualQueryResult2, aggregatedQueryResult, actualErrorQueryResults @ _*) = response.results

    actualQueryResult1 should equal(queryResult1)
    actualQueryResult2 should equal(queryResult2)

    val expectedAggregatedResult = queryResult1.withSetSize(totalSetSize).withInstanceId(queryId).withDescription("Aggregated Count")

    aggregatedQueryResult should equal(expectedAggregatedResult)

    actualErrorQueryResults.forall(qr => qr.description == Some("A"))
  }
}