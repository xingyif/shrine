package net.shrine.aggregation

import net.shrine.util.ShouldMatchersForJUnit
import org.junit.Test
import net.shrine.protocol.ReadQueryDefinitionResponse
import net.shrine.util.XmlDateHelper
import net.shrine.protocol.Result
import net.shrine.protocol.NodeId

/**
 * @author Bill Simons
 * @date 6/14/11
 * @link http://cbmi.med.harvard.edu
 * @link http://chip.org
 *       <p/>
 *       NOTICE: This software comes with NO guarantees whatsoever and is
 *       licensed as Lgpl Open Source
 * @link http://www.gnu.org/licenses/lgpl.html
 */
final class ReadQueryDefinitionAggregatorTest extends ShouldMatchersForJUnit {

  val aggregator = new ReadQueryDefinitionAggregator

  @Test
  def testAggregate {
    import scala.concurrent.duration._
    
    val queryId = 1L
    val userId = "userId"
    val queryName = "queryname"
    val queryDefinition = "<queryDef/>"
    
    val response1 = ReadQueryDefinitionResponse(queryId, queryName, userId, XmlDateHelper.now, queryDefinition)
    
    val result1 = Result(NodeId("X"), 1.second, response1)
    
    val response2 = ReadQueryDefinitionResponse(queryId, queryName, userId, XmlDateHelper.now, queryDefinition)
    
    val result2 = Result(NodeId("Y"), 1.second, response2)
    
    //TODO: test handling error responses
    val actual = aggregator.aggregate(Vector(result1, result2), Nil).asInstanceOf[ReadQueryDefinitionResponse]
    
    actual should not be(null)
    
    actual.masterId should equal(queryId)
    actual.name should equal(queryName)
    actual.userId should equal(userId)
    actual.queryDefinition should equal(queryDefinition)
  }
}
