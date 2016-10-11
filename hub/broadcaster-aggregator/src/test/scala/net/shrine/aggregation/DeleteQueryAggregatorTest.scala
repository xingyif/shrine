package net.shrine.aggregation

import org.junit.Test
import net.shrine.protocol.DeleteQueryResponse
import net.shrine.protocol.Result
import net.shrine.protocol.NodeId
import net.shrine.util.ShouldMatchersForJUnit

/**
 * @author Bill Simons
 * @date 8/16/11
 * @link http://cbmi.med.harvard.edu
 * @link http://chip.org
 *       <p/>
 *       NOTICE: This software comes with NO guarantees whatsoever and is
 *       licensed as Lgpl Open Source
 * @link http://www.gnu.org/licenses/lgpl.html
 */
final class DeleteQueryAggregatorTest extends ShouldMatchersForJUnit {

  @Test
  def testAggregate {
    import scala.concurrent.duration._
    
    val queryId = 12345l
    
    val response = DeleteQueryResponse(queryId)
    
    val result1 = Result(NodeId("A"), 1.day, response)
    val result2 = Result(NodeId("B"), 1.second, response)

    val aggregator = new DeleteQueryAggregator
    
    //TODO: test handling error responses
    val deleteQueryResponse = aggregator.aggregate(Vector(result1, result2), Nil,null).asInstanceOf[DeleteQueryResponse]
    
    deleteQueryResponse should not be(null)
    
    deleteQueryResponse.queryId should equal(queryId)
  }
}