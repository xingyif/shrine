package net.shrine.broadcaster

import java.util.concurrent.atomic.AtomicInteger

import net.shrine.log.Loggable

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.Buffer
import scala.concurrent.Future
import scala.concurrent.Promise
import scala.concurrent.duration.Duration
import scala.util.Try

import net.shrine.protocol.NodeId
import net.shrine.protocol.SingleNodeResult

/**
 * @author clint
 * @since Nov 15, 2013
 */
final class BufferingMultiplexer(broadcastTo: Set[NodeId]) extends Multiplexer with Loggable {

  //todo if this code survives, change to a BlockingQueue, use its size method, and get rid of this lock in 1.24
  private[this] val singleNodeResults: Buffer[SingleNodeResult] = new ArrayBuffer

  private[this] val heardFrom = new AtomicInteger
  
  private[this] val promise = Promise[Iterable[SingleNodeResult]]
  
  private[this] val lock = new AnyRef
  
  private[this] def locked[T](f: => T): T = lock.synchronized { f }
  
  def resultsSoFar: Iterable[SingleNodeResult] = locked { singleNodeResults.toIndexedSeq }
  
  def numHeardFrom: Int = heardFrom.get

  override def responses: Future[Iterable[SingleNodeResult]] = promise.future
  
  override def processResponse(response: SingleNodeResult): Unit = {

    val latestCount = locked {
      singleNodeResults += response
    
      heardFrom.incrementAndGet
    }
    
    debug(s"Latest result count: $latestCount")//todo ; handled response" $response")
    
    if(latestCount == broadcastTo.size) {
      //todo change to trySuccess or tryComplete
      promise.complete(Try(resultsSoFar))
    }
  }
}