package net.shrine.broadcaster

import net.shrine.util.ShouldMatchersForJUnit
import org.junit.Test
import net.shrine.protocol.NodeId
import net.shrine.protocol.DeleteQueryResponse
import net.shrine.protocol.Result
import scala.concurrent.Future
import scala.concurrent.Await

/**
 * @author clint
 * @date Dec 3, 2013
 */
final class BufferingMultiplexerTest extends ShouldMatchersForJUnit {
  @Test
  def testProcessResponse {
    val numNodes = 5
    
    val nodeIds = (1 to numNodes).map(i => NodeId(i.toString)).toSet
    
    val multiplexer = new BufferingMultiplexer(nodeIds)
    
    multiplexer.numHeardFrom should be(0)
    
    import scala.concurrent.duration._
    
    val resp = DeleteQueryResponse(12345L)
    
    val Seq(result0, result1, result2, result3, result4) = nodeIds.map(id => Result(id, 1.second, resp)).toSeq
    
    multiplexer.processResponse(result0)
    
    multiplexer.numHeardFrom should be(1)
    
    multiplexer.processResponse(result1)
    
    multiplexer.numHeardFrom should be(2)
    
    multiplexer.processResponse(result2)
    
    multiplexer.numHeardFrom should be(3)
    
    multiplexer.processResponse(result3)
    
    multiplexer.numHeardFrom should be(4)
    
    multiplexer.processResponse(result4)
    
    multiplexer.numHeardFrom should be(5)
    
    val responses = multiplexer.resultsSoFar
    
    responses.take(5) should equal(Seq(result0, result1, result2, result3, result4))
  }
  
  @Test
  def testProcessResponseConcurrently {
    val numNodes = 5
    
    val nodeIds = (1 to numNodes).map(i => NodeId(i.toString)).toSet
    
    val multiplexer = new BufferingMultiplexer(nodeIds)
    
    multiplexer.numHeardFrom should be(0)
    
    import scala.concurrent.duration._
    
    val resp = DeleteQueryResponse(12345L)
    
    val results = nodeIds.map(id => Result(id, 1.second, resp)).toSeq
    
    import scala.concurrent.ExecutionContext.Implicits._
    
    val futures = for {
      result <- results
    } yield Future {
      multiplexer.processResponse(result)
    }
    
    Await.result(Future.sequence(futures), 1.minute)
    
    multiplexer.numHeardFrom should be(5)
    
    val responses = multiplexer.resultsSoFar
    
    responses.take(5).toSet should equal(results.toSet)
  }
  
  @Test
  def testResponses {
    val numNodes = 5
    
    val nodeIds = (1 to numNodes).map(i => NodeId(i.toString)).toSet
    
    val multiplexer = new BufferingMultiplexer(nodeIds)
    
    multiplexer.numHeardFrom should be(0)
    
    import scala.concurrent.duration._
    
    val resp = DeleteQueryResponse(12345L)
    
    val results = nodeIds.map(id => Result(id, 1.second, resp)).toSeq
    
    import scala.concurrent.ExecutionContext.Implicits._
    
    val futures = for {
      result <- results
    } yield Future {
      Thread.sleep(1000)
      
      multiplexer.processResponse(result)
    }
    
    Await.result(Future.sequence(futures), 1.minute)
    
    val responses = Await.result(multiplexer.responses, 30.seconds)

    responses.size should be(5)
    
    responses.toSet should equal(results.toSet)
  }
  
  @Test
  def testResponsesTimeout {
    val numNodes = 5
    
    val nodeIds = (1 to numNodes).map(i => NodeId(i.toString)).toSet
    
    val multiplexer = new BufferingMultiplexer(nodeIds)
    
    multiplexer.numHeardFrom should be(0)
    
    import scala.concurrent.duration._
    
    val resp = DeleteQueryResponse(12345L)
    
    val results = nodeIds.map(id => Result(id, 1.second, resp)).toSeq
    
    import scala.concurrent.ExecutionContext.Implicits._
    
    val futures = (for {
      result <- results.take(4)
    } yield Future {
      Thread.sleep(100)
      
      multiplexer.processResponse(result)
    }) :+ Future {
      Thread.sleep(5000) //5 seconds
    }
    
    Thread.sleep(1000)
    
    val responses = multiplexer.resultsSoFar
    
    responses.size should be(4)
    
    responses.toSet should equal(results.take(4).toSet)
  }
}