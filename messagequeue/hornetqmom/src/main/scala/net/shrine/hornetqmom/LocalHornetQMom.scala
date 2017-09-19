package net.shrine.hornetqmom

import java.util.concurrent.{BlockingDeque, LinkedBlockingDeque, TimeUnit}

import com.typesafe.config.Config
import net.shrine.config.ConfigExtensions
import net.shrine.log.Log
import net.shrine.messagequeueservice.{Message, MessageQueueService, NoSuchQueueExistsInHornetQ, Queue}
import net.shrine.source.ConfigSource

import scala.collection.concurrent.{TrieMap, Map => ConcurrentMap}
import scala.collection.immutable.Seq
import scala.concurrent.blocking
import scala.concurrent.duration.Duration
import scala.util.{Success, Try}
/**
  * This object is the local version of the Message-Oriented Middleware API, which uses HornetQ service
  *
  * @author david
  * @since 7/18/17
  */

object LocalHornetQMom extends MessageQueueService {

  val config: Config = ConfigSource.config.getConfig("shrine.messagequeue.hornetq")

  /**
    * Use HornetQMomStopper to stop the hornetQServer without unintentially starting it
    */
  // todo drop this
  private[hornetqmom] def stop() = {
  }

  //todo key should be a Queue instead of a String
  val namesToShadowQueues:ConcurrentMap[String,BlockingDeque[String]] = TrieMap.empty

  //queue lifecycle
  def createQueueIfAbsent(queueName: String): Try[Queue] = Try {
    namesToShadowQueues.getOrElseUpdate(queueName, new LinkedBlockingDeque[String]())
    Queue(queueName)
  }

  def deleteQueue(queueName: String): Try[Unit] = Try{
    namesToShadowQueues.remove(queueName).getOrElse(throw new IllegalStateException(s"$queueName not found")) //todo this is actually fine - the state we want
  }

  override def queues: Try[Seq[Queue]] = Try{
    namesToShadowQueues.keys.map(Queue(_))(collection.breakOut)
  }

  //send a message
  def send(contents: String, to: Queue): Try[Unit] = Try{
    val queue = namesToShadowQueues.getOrElse(to.name,throw new IllegalStateException(s"queue $to not found"))
    queue.addLast(contents)
    Log.debug(s"After send to ${to.name} - shadowQueue ${queue.size} $queue")
  }

  //receive a message
  /**
    * Always do AWS SQS-style long polling.
    * Be sure your code can handle receiving the same message twice.
    *
    * @return Some message before the timeout, or None
    */
  def receive(from: Queue, timeout: Duration): Try[Option[Message]] = Try {
    val shadowQueue = namesToShadowQueues.getOrElse(from.name,throw new IllegalStateException(s"Queue $from not found")) //todo better exception
    Log.debug(s"Before receive from ${from.name} - shadowQueue ${shadowQueue.size} ${shadowQueue.toString}")
    blocking {
      val shadowMessage: Option[String] = Option(shadowQueue.pollFirst(timeout.toMillis, TimeUnit.MILLISECONDS))
      shadowMessage.map(SimpleMessage(_))
    }
  }

  //todo dead letter queue for all messages SHRINE-2261

  val unit = ()
  case class SimpleMessage(contents:String) extends Message {
    override def complete(): Try[Unit] = Success(unit) //todo fill this in when you build out complete
  }

}

/**
  * If the configuration is such that HornetQ should have been started use this object to stop it
  */
object LocalHornetQMomStopper {

  def stop(): Unit = {
    if(ConfigSource.config.getBoolean("shrine.messagequeue.hornetQWebApi.enabled")) {
      LocalHornetQMom.stop()
    }
  }

}