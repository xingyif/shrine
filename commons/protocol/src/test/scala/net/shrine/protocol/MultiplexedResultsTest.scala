package net.shrine.protocol

import net.shrine.util.ShouldMatchersForJUnit
import org.junit.Test

/**
 * @author clint
 * @date Mar 3, 2014
 */
final class MultiplexedResultsTest extends ShouldMatchersForJUnit {
  private val nodeId0 = NodeId("0")
  private val nodeId1 = NodeId("1")
  private val nodeId2 = NodeId("2")
  private val nodeId3 = NodeId("3")

  private val masterId = 12345L

  import scala.concurrent.duration._

  @Test
  def testXmlRoundTrip {
    val resultNodeId0 = Result(nodeId0, 1.second, DeleteQueryResponse(masterId))
    val timeoutNodeId1 = Timeout(nodeId1)
    val failureNodeId2 = FailureResult(nodeId2, new Exception("blarg") with scala.util.control.NoStackTrace)
    val resultNodeId3 = Result(nodeId3, 2.seconds, DeleteQueryResponse(masterId))

    val multiResults = MultiplexedResults(Seq(
      resultNodeId0,
      timeoutNodeId1,
      failureNodeId2,
      resultNodeId3))

    val xml = multiResults.toXml

    val unmarshalled = MultiplexedResults.fromXml(DefaultBreakdownResultOutputTypes.toSet)(xml).get

    val unmarshalledResults = unmarshalled.results.toSet

    unmarshalled.results(0) should equal(resultNodeId0)

    unmarshalled.results(1) should equal(timeoutNodeId1)

    unmarshalled.results(2).isInstanceOf[FailureResult] should equal(true)
    unmarshalled.results(2).asInstanceOf[FailureResult].origin should equal(nodeId2)
    unmarshalled.results(2).asInstanceOf[FailureResult].cause.getMessage.contains("blarg") should equal(true)

    unmarshalled.results(3) should equal(resultNodeId3)
  }
}