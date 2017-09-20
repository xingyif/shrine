package net.shrine.hornetqmom

import java.util
import java.util.UUID
import java.util.concurrent.{BlockingDeque, Executors, LinkedBlockingDeque, ScheduledExecutorService, ScheduledFuture, TimeUnit}

import akka.japi.Predicate
import com.typesafe.config.Config
import net.shrine.config.ConfigExtensions
import net.shrine.hornetqmom.LocalHornetQMom.{CleanDeliveryAttemptRunner, CleanInternalMessageInDequeRunner, MessageRedeliveryRunner}
import net.shrine.log.Log
import net.shrine.messagequeueservice.{Message, MessageQueueService, Queue}
import net.shrine.problem.{AbstractProblem, ProblemSources}
import net.shrine.source.ConfigSource
import org.json4s.ShortTypeHints
import org.json4s.native.Serialization

import scala.collection.concurrent.{TrieMap, Map => ConcurrentMap}
import scala.collection.immutable.Seq
import scala.concurrent.blocking
import scala.concurrent.duration.Duration
import scala.util.Try
import scala.util.control.NonFatal
/**
  * This object is the local version of the Message-Oriented Middleware API, which uses HornetQ service
  *
  * @author david
  * @since 7/18/17
  */

object LocalHornetQMom extends MessageQueueService {

  val configPath = "shrine.messagequeue.blockingq"
  val config = ConfigSource.config.getConfig(configPath)
  private val messageTimeToLiveInMillis: Long = config.get("messageTimeToLive", Duration(_)).toMillis
  private val messageRedeliveryDelay: Long = config.get("messageRedeliveryDelay", Duration(_)).toMillis
  private val messageMaxDeliveryAttempts: Int = config.getInt("messageMaxDeliveryAttempts")

  // keep a map of messages and ids
  private val messageDeliveryAttemptMap: TrieMap[UUID, DeliveryAttempt] = TrieMap.empty

  /**
    * Use HornetQMomStopper to stop the hornetQServer without unintentially starting it
    */
  private[hornetqmom] def stop() = {
    MessageScheduler.shutDown()
  }

  //todo key should be a Queue instead of a String SHRINE-2308
  //todo rename
  val blockingQueuePool:ConcurrentMap[String,BlockingDeque[InternalToBeSentMessage]] = TrieMap.empty

  //queue lifecycle
  def createQueueIfAbsent(queueName: String): Try[Queue] = Try {
    blockingQueuePool.getOrElseUpdate(queueName, new LinkedBlockingDeque[InternalToBeSentMessage]())
    Queue(queueName)
  }

  def deleteQueue(queueName: String): Try[Unit] = Try{
    blockingQueuePool.remove(queueName).getOrElse(throw QueueDoesNotExistException(Queue(queueName)))
  }

  override def queues: Try[Seq[Queue]] = Try{
    blockingQueuePool.keys.map(Queue(_))(collection.breakOut)
  }

  //send a message
  def send(contents: String, to: Queue): Try[Unit] = Try {
    // schedule future cleanup when the message expires
    MessageScheduler.scheduleCleanupDeliveryAttemptInMap(messageTimeToLiveInMillis)
    // send message to queue
    val queue = blockingQueuePool.getOrElse(to.name, throw QueueDoesNotExistException(to))
    // creates a message
    val msgID: UUID = UUID.randomUUID()
    val internalMessage: InternalToBeSentMessage = InternalToBeSentMessage(msgID, contents, System.currentTimeMillis(), to)
    // schedule future cleanup of this message in blockingQueue if client never call receive
    MessageScheduler.scheduleCleanupMessageInDeque(internalMessage, messageTimeToLiveInMillis)
    // waiting if necessary for space to become available
    blocking(queue.putLast(internalMessage))
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
    val shadowQueue = blockingQueuePool.getOrElse(from.name,throw QueueDoesNotExistException(from))
    Log.debug(s"Before receive from ${from.name} - shadowQueue ${shadowQueue.size} ${shadowQueue.toString}")
    val shadowMessage: Option[InternalToBeSentMessage] = Option(shadowQueue.pollFirst(timeout.toMillis, TimeUnit.MILLISECONDS))
    val messageOpt: Option[Message] = shadowMessage.map { internalToBeSentMessage: InternalToBeSentMessage =>
      // add a deliveryAttempt in the map with an unique UUID
      val deliveryAttemptID = UUID.randomUUID()
      val simpleMessage: SimpleMessage = SimpleMessage(deliveryAttemptID.toString, internalToBeSentMessage.contents)
      val deliveryAttempt: DeliveryAttempt = DeliveryAttempt(internalToBeSentMessage, System.currentTimeMillis(), from)
      messageDeliveryAttemptMap.update(deliveryAttemptID, deliveryAttempt)
      MessageScheduler.scheduleMessageRedelivery(deliveryAttemptID, deliveryAttempt, messageRedeliveryDelay, messageMaxDeliveryAttempts)
      simpleMessage
    }
    messageOpt
  }

  def completeMessage(deliveryAttemptID: UUID): Try[Unit] = Try {
    // cancel message redelivery scheduled task
    println(s"in completeMessage, about to cancel $deliveryAttemptID")
    val canceled = MessageScheduler.cancelScheduledMessageRedelivery(deliveryAttemptID)
    println(s"canceled message redelivery: $canceled")
    // removes the message from the map
    messageDeliveryAttemptMap.remove(deliveryAttemptID)
  }

  //todo dead letter queue for all messages SHRINE-2261

  case class SimpleMessage private(deliveryAttemptID: String, content: String) extends Message {
    def toJson: String = {
      Serialization.write(this)(SimpleMessage.messageFormats)
    }
    override def complete(): Try[Unit] = Try {
      val uuid: UUID = UUID.fromString(deliveryAttemptID)
      completeMessage(uuid) // todo client should never be able to get to here, should we throw exception/log a problem if this function is triggered?
    }

    override def deliveryAttemptUUID: UUID = UUID.fromString(deliveryAttemptID)

    override def contents: String = this.content
  }

  object SimpleMessage {
    val messageFormats = Serialization.formats(ShortTypeHints(List(classOf[SimpleMessage])))

    def fromJson(jsonString: String): SimpleMessage = {
      implicit val formats = messageFormats
      Serialization.read[SimpleMessage](jsonString)
    }
  }

  case class CleanDeliveryAttemptRunner(messageTimeToLiveInMillis: Long) extends Runnable {
    // watches the map
    override def run(): Unit = {
      try {
        val currentTimeInMillis: Long = System.currentTimeMillis()
        Log.debug("About to clean up outstanding messages.")
        // cleans up deliveryAttempt map
        messageDeliveryAttemptMap.retain({ (uuid, deliveryAttempt: DeliveryAttempt) =>
          (currentTimeInMillis - deliveryAttempt.createdTime) < messageTimeToLiveInMillis
        })
        // cleans up the deque in case no one called receive

        println(s"Outstanding deliveryAttempts that exceed $messageTimeToLiveInMillis milliseconds have been cleaned from the map.")
        Log.debug(s"Outstanding deliveryAttempts that exceed $messageTimeToLiveInMillis milliseconds have been cleaned from the map.")
      } catch {
        case NonFatal(x) => CleaningUpDeliveryAttemptProblem(messageTimeToLiveInMillis, x)
        //pass-through to blow up the thread, receive no more results, do something dramatic in UncaughtExceptionHandler.
        case x => Log.error("Fatal exception while cleaning up outstanding messages", x)
          throw x
      }
    }
  }

  case class MessageRedeliveryRunner(deliveryAttemptID: UUID, deliveryAttempt: DeliveryAttempt, messageRedeliveryDelay: Long, messageMaxDeliveryAttempts: Long) extends Runnable {
    override def run(): Unit = {
      try {
        messageDeliveryAttemptMap.remove(deliveryAttemptID).fold(){ deliveryAttempt =>
          // get the queue that the message was from, and push message back to the head of the deque
          val shadowQueue = blockingQueuePool.getOrElse(deliveryAttempt.fromQueue.name,
            // todo message has been completed
            throw QueueDoesNotExistException(deliveryAttempt.fromQueue))
          // waiting if necessary for space to become available
          println("in localhornetq, removed da from map and putting the message back to it's queue head")
          blocking(shadowQueue.putFirst(deliveryAttempt.message))
        }
      }
    }
  }

  case class CleanInternalMessageInDequeRunner(messageToBeRemoved: InternalToBeSentMessage, messageTimeToLiveInMillis: Long) extends Runnable {
    override def run(): Unit = {
      try {
        val shadowQueue = blockingQueuePool.getOrElse(messageToBeRemoved.toQueue.name,
          throw QueueDoesNotExistException(messageToBeRemoved.toQueue))
        println(shadowQueue.remove(messageToBeRemoved))
        Log.debug(s"Removed internalMessage from it's queue ${messageToBeRemoved.toQueue}" +
          s" because it exceeds expiration time $messageTimeToLiveInMillis millis")
      }
    }
  }

}

object MessageScheduler {
  private val redeliveryScheduler: ScheduledExecutorService = Executors.newScheduledThreadPool(1)
  private val cleanupScheduler: ScheduledExecutorService = Executors.newScheduledThreadPool(1)

  def scheduleMessageRedelivery(deliveryAttemptID: UUID, deliveryAttempt: DeliveryAttempt, messageRedeliveryDelay: Long, messageMaxDeliveryAttempts: Int) = {
    val messageRedeliveryRunner: MessageRedeliveryRunner = MessageRedeliveryRunner(deliveryAttemptID, deliveryAttempt, messageRedeliveryDelay, messageMaxDeliveryAttempts)
    try {
      Log.debug(s"Scheduling message redelivery, redeliver message in $messageRedeliveryDelay milliseconds, maximum redeliver time $messageMaxDeliveryAttempts")
      if (messageMaxDeliveryAttempts == -1) {
        // infinite
        redeliveryScheduler.scheduleAtFixedRate(messageRedeliveryRunner, messageRedeliveryDelay, messageRedeliveryDelay, TimeUnit.MILLISECONDS)
      } else {
        for (i <- 1 to messageMaxDeliveryAttempts) {
          println(s"in scheduler, current attempt is $i")
          Log.debug(s"Scheduling message redelivery attempt $i, redeliver message in $messageRedeliveryDelay milliseconds")
          redeliveryScheduler.schedule(messageRedeliveryRunner, messageRedeliveryDelay * i, TimeUnit.MILLISECONDS)
        }
      }
    } catch {
      case NonFatal(x) => SchedulingMessageRedeliverySentinelProblem(messageRedeliveryDelay, x)
      //pass-through to blow up the thread, receive no more results, do something dramatic in UncaughtExceptionHandler.
      case x => Log.error("Fatal exception while scheduling a sentinel for cleaning up outstanding messages", x)
        throw x
    }
  }

  def scheduleCleanupDeliveryAttemptInMap(messageTimeToLiveInMillis: Long) = {

    // a sentinel that monitors the hashmap of messageDeliveryAttemptMap, any message that has been outstanding will be permanently removed
    // time-to-live need to get cleaned out of this map
    val cleanDeliveryAttemptRunner: CleanDeliveryAttemptRunner = CleanDeliveryAttemptRunner(messageTimeToLiveInMillis)
    try {
      Log.debug(s"Starting the sentinel scheduler that cleans outstanding deliveryAttempt exceeds message expiration time: $messageTimeToLiveInMillis")
      cleanupScheduler.schedule(cleanDeliveryAttemptRunner, messageTimeToLiveInMillis, TimeUnit.MILLISECONDS)
    } catch {
      case NonFatal(x) => SchedulingCleanUpSentinelProblem(messageTimeToLiveInMillis, x)
      //pass-through to blow up the thread, receive no more results, do something dramatic in UncaughtExceptionHandler.
      case x => Log.error("Fatal exception while scheduling a sentinel for cleaning up outstanding messages", x)
        throw x
    }
  }

  def scheduleCleanupMessageInDeque(messageToBeRemoved: InternalToBeSentMessage, messageTimeToLiveInMillis: Long) = {
    val cleanInternalMessageInDequeRunner: CleanInternalMessageInDequeRunner = CleanInternalMessageInDequeRunner(messageToBeRemoved, messageTimeToLiveInMillis)
    try {
      Log.debug(s"Starting the sentinel scheduler that cleans outstanding internal message in" +
        s" queue ${messageToBeRemoved.toQueue} exceeds message expiration time: $messageTimeToLiveInMillis")
      cleanupScheduler.schedule(cleanInternalMessageInDequeRunner, messageTimeToLiveInMillis, TimeUnit.MILLISECONDS)
    } catch {
      case NonFatal(x) => SchedulingCleanUpSentinelProblem(messageTimeToLiveInMillis, x)
      //pass-through to blow up the thread, receive no more results, do something dramatic in UncaughtExceptionHandler.
      case x => Log.error("Fatal exception while scheduling a sentinel for cleaning up outstanding messages", x)
        throw x
    }
  }


  def cancelScheduledMessageRedelivery(deliveryAttemptID: UUID): Unit = {
    // returns false if the task could not be cancelled, typically because it has already completed normally;
    redeliveryScheduler.shutdownNow()
  }

  def shutDown(): util.List[Runnable] = {
    redeliveryScheduler.shutdownNow()
    cleanupScheduler.shutdownNow()
  }

}

case class InternalToBeSentMessage(id: UUID, contents: String, createdTime: Long, toQueue: Queue) {
  override def equals(obj: scala.Any): Boolean = {
    obj match {
      case other: InternalToBeSentMessage =>
        other.toQueue.equals(this.toQueue) && other.id.equals(this.id) && other.createdTime.equals(this.createdTime) &&
          other.contents.equals(this.contents)
      case _ =>
        false
    }
  }
}

case class DeliveryAttempt(message: InternalToBeSentMessage, createdTime: Long, fromQueue: Queue)

/**
  * If the configuration is such that HornetQ should have been started use this object to stop it
  */
object LocalHornetQMomStopper {

  def stop(): Unit = {
    //a lot less interesting without hornetq - not a big deal to stop schedulers that were never started , maybe nothing to do.
    LocalHornetQMom.stop()
  }

}

case class CleaningUpDeliveryAttemptProblem(timeOutInMillis: Long, x:Throwable) extends AbstractProblem(ProblemSources.Hub) {

  override val throwable = Some(x)

  override def summary: String = s"The Hub encountered an exception while trying to " +
    s"cleanup messages that has been outstanding for more than $timeOutInMillis milliseconds"

  override def description: String = s"The Hub encountered an exception while trying to " +
    s"cleanup messages that has been outstanding for more than $timeOutInMillis milliseconds" +
    s"on Thread ${Thread.currentThread().getName}: ${x.getMessage}"
}

case class SchedulingCleanUpSentinelProblem(timeOutInMillis: Long, x:Throwable) extends AbstractProblem(ProblemSources.Hub) {
  override val throwable = Some(x)

  override def summary: String = s"The Hub encountered an exception while trying to " +
    s"schedule a sentinel that cleans up outstanding messages exceed $timeOutInMillis milliseconds"

  override def description: String = s"The Hub encountered an exception while trying to " +
    s"schedule a sentinel that cleans up outstanding messages exceed $timeOutInMillis milliseconds " +
    s"on Thread ${Thread.currentThread().getName}: ${x.getMessage}"
}

case class SchedulingMessageRedeliverySentinelProblem(messageRedeliveryDelay: Long, x:Throwable) extends AbstractProblem(ProblemSources.Hub) {
  override val throwable = Some(x)

  override def summary: String = s"The Hub encountered an exception while trying to " +
    s"schedule a sentinel that redelivers an incomplete message after $messageRedeliveryDelay milliseconds"

  override def description: String = s"The Hub encountered an exception while trying to " +
    s"schedule a sentinel that redelivers an incomplete message after $messageRedeliveryDelay milliseconds" +
    s"on Thread ${Thread.currentThread().getName}: ${x.getMessage}"
}

case class QueueDoesNotExistException(queueName: Queue) extends Exception(s"Cannot match given ${queueName.name}" +
  s" to any Queue in the server! Queue does not exist!")

case class MessageDoesNotExistAndCannotBeCompletedException(id: UUID) extends Exception(s"Cannot match given " +
  s"deliveryAttemptID ${id.toString} to any Message in the server! Message does not exist and cannot be completed!")
