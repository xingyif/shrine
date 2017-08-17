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
 * @date Nov 15, 2013
 */
final class BufferingMultiplexer(broadcastTo: Set[NodeId]) extends Multiplexer with Loggable {

  //todo rename for SHRINE-2120
  //todo if this code survives, change to a BlockingQueue and get rid of this lock
  private[this] val queue: Buffer[SingleNodeResult] = new ArrayBuffer

  private[this] val heardFrom = new AtomicInteger
  
  private[this] val promise = Promise[Iterable[SingleNodeResult]]
  
  private[this] val lock = new AnyRef
  
  private[this] def locked[T](f: => T): T = lock.synchronized { f }
  
  def resultsSoFar: Iterable[SingleNodeResult] = locked { queue.toIndexedSeq }
  
  def numHeardFrom: Int = heardFrom.get

  override def responses: Future[Iterable[SingleNodeResult]] = promise.future
  
  override def processResponse(response: SingleNodeResult): Unit = {

    val latestCount = locked {
      queue += response 
    
      heardFrom.incrementAndGet
    }
    
    debug(s"Latest result count: $latestCount")//todo ; handled response" $response")
    
    if(latestCount == broadcastTo.size) {
      //todo change to trySuccess or tryComplete
      promise.complete(Try(resultsSoFar))
    }
  }
}