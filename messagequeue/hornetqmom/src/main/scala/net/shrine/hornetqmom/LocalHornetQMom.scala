package net.shrine.hornetqmom

import java.util
import java.util.UUID
import java.util.concurrent.{BlockingDeque, Executors, LinkedBlockingDeque, ScheduledExecutorService, TimeUnit}

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
  def config = ConfigSource.config.getConfig(configPath)
  private def messageTimeToLiveInMillis: Long = config.get("messageTimeToLive", Duration(_)).toMillis
  private def messageRedeliveryDelay: Long = config.get("messageRedeliveryDelay", Duration(_)).toMillis
  private def messageMaxDeliveryAttempts: Int = config.getInt("messageMaxDeliveryAttempts")

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
    val queue = blockingQueuePool.getOrElse(to.name, throw QueueDoesNotExistException(to))
    // schedule future cleanup when the message expires
    MessageScheduler.scheduleCleanupDeliveryAttemptInMap(to, messageTimeToLiveInMillis)
    // creates a message
    val msgID: UUID = UUID.randomUUID()
    val internalMessage: InternalToBeSentMessage = InternalToBeSentMessage(msgID, contents, System.currentTimeMillis(), to)
    // schedule future cleanup of this message in blockingQueue if client never call receive
    MessageScheduler.scheduleCleanupMessageInDeque(to, internalMessage, messageTimeToLiveInMillis)
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
    // poll the first message from the blocking deque
    val shadowQueue = blockingQueuePool.getOrElse(from.name, throw QueueDoesNotExistException(from))
    Log.debug(s"Before receive from ${from.name} - shadowQueue ${shadowQueue.size} ${shadowQueue.toString}")
    val shadowMessage: Option[InternalToBeSentMessage] = Option(shadowQueue.pollFirst(timeout.toMillis, TimeUnit.MILLISECONDS))
    val deliveryAttemptID = UUID.randomUUID()
    val deliveryAttemptOpt: Option[DeliveryAttempt] = shadowMessage.map { internalToBeSentMessage: InternalToBeSentMessage =>
      DeliveryAttempt(internalToBeSentMessage, System.currentTimeMillis(), from)
    }
    // add a deliveryAttempt in the DAmap with an unique UUID
    deliveryAttemptOpt.fold(
      // No message available from the queue
      Log.debug(s"No message available from the queue ${from.name}")
    ){ deliveryAttempt: DeliveryAttempt =>
      messageDeliveryAttemptMap.update(deliveryAttemptID, deliveryAttempt)
      MessageScheduler.scheduleMessageRedelivery(deliveryAttemptID, deliveryAttempt, messageRedeliveryDelay, messageMaxDeliveryAttempts)
    }
    val messageOpt: Option[Message] = shadowMessage.map { internalToBeSentMessage: InternalToBeSentMessage =>
      val simpleMessage: SimpleMessage = SimpleMessage(deliveryAttemptID.toString, internalToBeSentMessage.contents)
      simpleMessage
    }
    messageOpt
  }

  def completeMessage(deliveryAttemptID: UUID): Try[Unit] = Try {
    // cancel message redelivery scheduled task
    MessageScheduler.cancelScheduledMessageRedelivery(deliveryAttemptID)
    // removes the message from the map
    messageDeliveryAttemptMap.remove(deliveryAttemptID).fold(
      throw MessageDoesNotExistAndCannotBeCompletedException(deliveryAttemptID)) { deliveryAttempt: DeliveryAttempt =>
      Log.debug(s"Message redelivery was canceled!")
      deliveryAttempt
    }
  }

  //todo dead letter queue for all messages SHRINE-2261

  case class SimpleMessage (deliveryAttemptID: String, content: String) extends Message {
    def toJson: String = {
      Serialization.write(this)(SimpleMessage.messageFormats)
    }
    override def complete(): Try[Unit] = Try {
      val uuid: UUID = UUID.fromString(deliveryAttemptID)
      LocalHornetQMom.completeMessage(uuid)
    }

    override def contents: String = this.content
  }

  object SimpleMessage {
    val messageFormats = Serialization.formats(ShortTypeHints(List(classOf[SimpleMessage])))

    def fromJson(jsonString: String): SimpleMessage = {
      implicit val formats = messageFormats
      Serialization.read[SimpleMessage](jsonString)
    }
  }

  case class CleanDeliveryAttemptRunner(queue: Queue, messageTimeToLiveInMillis: Long) extends Runnable {
    // watches the map
    override def run(): Unit = {
      try {
        val currentTimeInMillis: Long = System.currentTimeMillis()
        Log.debug(s"About to clean up outstanding messages. DAMap size: ${messageDeliveryAttemptMap.size}")
        // cleans up deliveryAttempt map
        messageDeliveryAttemptMap.retain({ (uuid, deliveryAttempt: DeliveryAttempt) =>
          (currentTimeInMillis - deliveryAttempt.createdTime) < messageTimeToLiveInMillis
        })
        Log.debug(s"Outstanding deliveryAttempts that exceed $messageTimeToLiveInMillis milliseconds have been cleaned from the map. " +
          s"DAMap size: ${messageDeliveryAttemptMap.size}")
      } catch {
        case NonFatal(x) => CleaningUpDeliveryAttemptProblem(queue, messageTimeToLiveInMillis, x)
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
          Log.debug("About to redelivery a message. Inserted the message back to the head of its queue!")
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
        val removed: Boolean = shadowQueue.remove(messageToBeRemoved)
        Log.debug(s"$removed: Removed internalMessage from it's queue ${messageToBeRemoved.toQueue}" +
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
      val currentAttempt = deliveryAttempt.message.getCurrentAttempt
      if (currentAttempt < messageMaxDeliveryAttempts) {
        Log.debug(s"Scheduling message redelivery attempt $currentAttempt, redeliver message in $messageRedeliveryDelay milliseconds.")
        redeliveryScheduler.schedule(messageRedeliveryRunner, messageRedeliveryDelay, TimeUnit.MILLISECONDS)
      } else {
        Log.debug(s"Not scheduling message redelivery because currentAttempt $currentAttempt reached max attempt number $messageMaxDeliveryAttempts.")
      }
    } catch {
      case NonFatal(x) => SchedulingMessageRedeliverySentinelProblem(messageRedeliveryDelay, x)
      //pass-through to blow up the thread, receive no more results, do something dramatic in UncaughtExceptionHandler.
      case x => Log.error("Fatal exception while scheduling a sentinel for cleaning up outstanding messages", x)
        throw x
    }
  }

  def scheduleCleanupDeliveryAttemptInMap(queue: Queue, messageTimeToLiveInMillis: Long) = {

    // a sentinel that monitors the hashmap of messageDeliveryAttemptMap, any message that has been outstanding will be permanently removed
    // time-to-live need to get cleaned out of this map
    val cleanDeliveryAttemptRunner: CleanDeliveryAttemptRunner = CleanDeliveryAttemptRunner(queue, messageTimeToLiveInMillis)
    try {
      Log.debug(s"Starting the sentinel scheduler that cleans outstanding deliveryAttempt exceeds message expiration time: $messageTimeToLiveInMillis")
      cleanupScheduler.schedule(cleanDeliveryAttemptRunner, messageTimeToLiveInMillis, TimeUnit.MILLISECONDS)
    } catch {
      case NonFatal(x) => SchedulingCleanUpSentinelProblem(queue, messageTimeToLiveInMillis, x)
      //pass-through to blow up the thread, receive no more results, do something dramatic in UncaughtExceptionHandler.
      case x => Log.error("Fatal exception while scheduling a sentinel for cleaning up outstanding messages", x)
        throw x
    }
  }

  def scheduleCleanupMessageInDeque(queue: Queue, messageToBeRemoved: InternalToBeSentMessage, messageTimeToLiveInMillis: Long) = {
    val cleanInternalMessageInDequeRunner: CleanInternalMessageInDequeRunner = CleanInternalMessageInDequeRunner(messageToBeRemoved, messageTimeToLiveInMillis)
    try {
      Log.debug(s"Starting the sentinel scheduler that cleans outstanding internal message in" +
        s" queue ${messageToBeRemoved.toQueue} exceeds message expiration time: $messageTimeToLiveInMillis")
      cleanupScheduler.schedule(cleanInternalMessageInDequeRunner, messageTimeToLiveInMillis, TimeUnit.MILLISECONDS)
    } catch {
      case NonFatal(x) => SchedulingCleanUpSentinelProblem(queue, messageTimeToLiveInMillis, x)
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
        other.canEqual(this) && super.equals(other)
      case _ => false
    }
  }
  private var currentAttempt = 0
  def getCurrentAttempt: Int = currentAttempt
  def incrementCurrentAttempt(): Unit = {
    currentAttempt = currentAttempt + 1
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

case class CleaningUpDeliveryAttemptProblem(queue: Queue, timeOutInMillis: Long, x:Throwable) extends AbstractProblem(ProblemSources.Hub) {

  override val throwable = Some(x)

  override def summary: String = s"The Hub encountered an exception while trying to " +
    s"cleanup messages that has been outstanding for more than $timeOutInMillis milliseconds in queue ${queue.name}. "

  override def description: String = s"The Hub encountered an exception while trying to " +
    s"cleanup messages that has been outstanding for more than $timeOutInMillis milliseconds in queue ${queue.name} " +
    s"on Thread ${Thread.currentThread().getName}: ${x.getMessage}"
}

case class SchedulingCleanUpSentinelProblem(queue: Queue, timeOutInMillis: Long, x:Throwable) extends AbstractProblem(ProblemSources.Hub) {
  override val throwable = Some(x)

  override def summary: String = s"The Hub encountered an exception while trying to " +
    s"schedule a sentinel that cleans up outstanding messages exceed $timeOutInMillis milliseconds in queue ${queue.name}."

  override def description: String = s"The Hub encountered an exception while trying to " +
    s"schedule a sentinel that cleans up outstanding messages exceed $timeOutInMillis milliseconds  in queue ${queue.name} " +
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

case class MessageDoesNotExistAndCannotBeCompletedException(id: UUID) extends Exception(s"Message does not exist" +
  s" and cannot be completed! Message might have already been completed or expired!")
