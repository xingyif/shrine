package net.shrine.aggregation

import net.shrine.util.ShouldMatchersForJUnit
import org.junit.Test
import net.shrine.protocol.FlagQueryResponse
import net.shrine.protocol.Result
import net.shrine.protocol.NodeId
import net.shrine.protocol.ErrorResponse

/**
 * @author clint
 * @date Mar 27, 2014
 */
final class FlagQueryAggregatorTest extends ShouldMatchersForJUnit {
  @Test
  def testAggregate {
    import scala.concurrent.duration._
    
    val networkQueryId = 12345L
    
    val result1 = Result(NodeId("A"), 1.day, FlagQueryResponse)
    val result2 = Result(NodeId("B"), 1.second, FlagQueryResponse)

    val aggregator = new FlagQueryAggregator
    
    //TODO: test handling error responses
    val aggregatedResponse = aggregator.aggregate(Vector(result1, result2), Nil).asInstanceOf[FlagQueryResponse]
    
    aggregatedResponse should not be(null)
    
    aggregatedResponse should equal(FlagQueryResponse)
  }
  
  @Test
  def testNoResults: Unit = {
    (new FlagQueryAggregator).aggregate(Nil, Nil).isInstanceOf[ErrorResponse] should be(true)
  }
}