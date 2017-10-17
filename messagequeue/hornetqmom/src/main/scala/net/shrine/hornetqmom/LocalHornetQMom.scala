package net.shrine.hornetqmom

import java.util
import java.util.UUID
import java.util.concurrent.{BlockingDeque, Executors, LinkedBlockingDeque, ScheduledFuture, TimeUnit}

import net.shrine.config.ConfigExtensions
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
import scala.util.control.NonFatal
import scala.util.{Success, Try}
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
  private val messageDeliveryAttemptMap: TrieMap[UUID, (DeliveryAttempt, ScheduledFuture[_])] = TrieMap.empty

  /**
    * Use HornetQMomStopper to stop the hornetQServer without unintentially starting it
    */
  private[hornetqmom] def stop() = {
    MessageScheduler.shutDown()
  }

  //todo key should be a Queue instead of a String SHRINE-2308
  //todo rename
  val blockingQueuePool: ConcurrentMap[String, BlockingDeque[InternalMessage]] = TrieMap.empty

  //queue lifecycle
  def createQueueIfAbsent(queueName: String): Try[Queue] = Try {
    blockingQueuePool.getOrElseUpdate(queueName, new LinkedBlockingDeque[InternalMessage]())
    Queue(queueName)
  }

  def deleteQueue(queueName: String): Try[Unit] = Try {
    blockingQueuePool.remove(queueName).getOrElse(throw QueueDoesNotExistException(Queue(queueName)))
  }

  override def queues: Try[Seq[Queue]] = Try {
    blockingQueuePool.keys.map(Queue(_))(collection.breakOut)
  }

  //send a message
  def send(contents: String, to: Queue): Try[Unit] = Try {
    val queue = blockingQueuePool.getOrElse(to.name, throw QueueDoesNotExistException(to))
    // creates a message
    val msgID: UUID = UUID.randomUUID()
    val internalMessage: InternalMessage = InternalMessage(msgID, contents, System.currentTimeMillis(), to, 0)
    // schedule future cleanup when the message expires
    MessageScheduler.scheduleMessageExpiry(to, internalMessage, messageTimeToLiveInMillis)
    // waiting if necessary for space to become available
    blocking(queue.putLast(internalMessage))
    Log.debug(s"After send to ${to.name} - blockingQueue ${queue.size} $queue")
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
    val blockingQueue = blockingQueuePool.getOrElse(from.name, throw QueueDoesNotExistException(from))
    Log.debug(s"Before receive from ${from.name} - blockingQueue ${blockingQueue.size} ${blockingQueue.toString}")
    val internalMessage: Option[InternalMessage] = Option(blockingQueue.pollFirst(timeout.toMillis, TimeUnit.MILLISECONDS))
    val deliveryAttemptID = UUID.randomUUID()
    val deliveryAttemptOpt: Option[DeliveryAttempt] = internalMessage.map { internalToBeSentMessage: InternalMessage =>
      DeliveryAttempt(internalToBeSentMessage, internalToBeSentMessage.createdTime, from)
    }
    // add a deliveryAttempt in the DAmap with an unique UUID
    deliveryAttemptOpt.fold(
      // No message available from the queue
      Log.debug(s"No message available from the queue ${from.name}")
    ) { deliveryAttempt: DeliveryAttempt =>
      MessageScheduler.scheduleMessageRedelivery(deliveryAttemptID, deliveryAttempt, messageRedeliveryDelay, messageMaxDeliveryAttempts)
    }
    val messageOpt: Option[Message] = internalMessage.map { internalToBeSentMessage: InternalMessage =>
      val simpleMessage: SimpleMessage = SimpleMessage(deliveryAttemptID.toString, internalToBeSentMessage.contents)
      simpleMessage
    }
    messageOpt
  }

  def completeMessage(deliveryAttemptID: UUID): Try[Unit] = Try {
    val deliveryAttemptAndFutureTaskOpt: Option[(DeliveryAttempt, ScheduledFuture[_])] = messageDeliveryAttemptMap.get(deliveryAttemptID)
    deliveryAttemptAndFutureTaskOpt.fold(
      // if message delivery attempt does not exist in the map, then it might be in the queue or expired
      throw MessageDoesNotExistAndCannotBeCompletedException(deliveryAttemptID)
    )
    { (deliveryAttemptAndFutureTask: (DeliveryAttempt, ScheduledFuture[_])) =>
      val deliveryAttempt: DeliveryAttempt = deliveryAttemptAndFutureTask._1
      val internalToBeSentMessage: InternalMessage = deliveryAttempt.message
      val queue: Queue = internalToBeSentMessage.toQueue
      // removes all deliveryAttempts of the message from the map and cancels all the scheduled redelivers
      for ((uuid: UUID, eachDAandTask: (DeliveryAttempt, ScheduledFuture[_])) <- messageDeliveryAttemptMap) {
        if (eachDAandTask._1.message.id == internalToBeSentMessage.id) {
          messageDeliveryAttemptMap.remove(uuid)
          // cancel message redelivery scheduled task
          MessageScheduler.cancelScheduledMessageRedelivery(eachDAandTask._2)
        }
      }
      // removed the message from the queue (if it exists)
      // i.e: if completeMsg is called after it is redelivered, message is back
      // in queue and waiting to be redelivered again
      val blockingQueue = blockingQueuePool.getOrElse(queue.name, throw QueueDoesNotExistException(queue))
      blockingQueue.remove(internalToBeSentMessage)
      Log.debug(s"Message from ${deliveryAttemptAndFutureTask._1.fromQueue} is completed and its redelivery was canceled!")
      Success(deliveryAttemptAndFutureTask._1)
    }
  }

  //todo dead letter queue for all messages SHRINE-2261

  case class SimpleMessage(deliveryAttemptID: String, content: String) extends Message {
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

  case class CleanExpiredDeliveryAttemptRunner(queue: Queue, messageTimeToLiveInMillis: Long) extends Runnable {
    // watches the map
    override def run(): Unit = {
      try {
        val currentTimeInMillis: Long = System.currentTimeMillis()
        Log.debug(s"About to clean up outstanding messages. DAMap size: ${messageDeliveryAttemptMap.size}")
        // cleans up deliveryAttempt map
        for ((uuid: UUID, deliveryAttemptAndFutureTask: (DeliveryAttempt, ScheduledFuture[_])) <- messageDeliveryAttemptMap){
          if ((currentTimeInMillis - deliveryAttemptAndFutureTask._1.message.createdTime) >= messageTimeToLiveInMillis) {
            messageDeliveryAttemptMap.remove(uuid, deliveryAttemptAndFutureTask)
            // cancels its future message redelivery task
            MessageScheduler.cancelScheduledMessageRedelivery(deliveryAttemptAndFutureTask._2)
          }
        }
        Log.debug(s"Outstanding deliveryAttempts that exceed $messageTimeToLiveInMillis milliseconds have been cleaned from the map. " +
          s"DAMap size: ${messageDeliveryAttemptMap.size}")
      } catch {
        case NonFatal(x) => CleaningUpDeliveryAttemptProblem(queue, messageTimeToLiveInMillis, x)
        case i: InterruptedException => Log.error("Scheduled expired message cleanup was interrupted", i)
        case t: TimeoutException => Log.error(s"Expired Messages can't be cleaned due to timeout", t)
        case e: Throwable => Log.error(s"""${e.getClass.getSimpleName} "${e.getMessage}" caught by exception handler""", e)
      }
    }
  }

  case class MessageRedeliveryRunner(deliveryAttemptID: UUID, deliveryAttempt: DeliveryAttempt, messageRedeliveryDelay: Long, messageMaxDeliveryAttempts: Long) extends Runnable {
    override def run(): Unit = {
      try {
        messageDeliveryAttemptMap.get(deliveryAttemptID).fold(
                  Log.debug(s"Could not find deliveryAttempt for message ${deliveryAttempt.message.contents} from queue ${deliveryAttempt.fromQueue}")
        ) { (deliveryAttemptAndFutureTask: (DeliveryAttempt, ScheduledFuture[_]))  =>
          // get the queue that the message was from, and push message back to the head of the deque
          val blockingQueue = blockingQueuePool.getOrElse(deliveryAttemptAndFutureTask._1.fromQueue.name,
            throw QueueDoesNotExistException(deliveryAttemptAndFutureTask._1.fromQueue))
          // waiting if necessary for space to become available
          Log.debug("About to redelivery a message. Inserted the message back to the head of its queue!")
          val message = deliveryAttemptAndFutureTask._1.message.copy(currentAttemptCount = deliveryAttemptAndFutureTask._1.message.currentAttemptCount + 1)
          blocking(blockingQueue.putFirst(message))
        }
      } catch {
        case i: InterruptedException => Log.error("Scheduled message redelivery was interrupted", i)
        case t: TimeoutException => Log.error(s"Messages can't be redelivered due to timeout", t)
        case e: Throwable => Log.error(s"""${e.getClass.getSimpleName} "${e.getMessage}" caught by exception handler""", e)
      }
    }
  }

  case class CleanInternalMessageInDequeRunner(messageToBeRemoved: InternalMessage, messageTimeToLiveInMillis: Long) extends Runnable {
    override def run(): Unit = {
      try {
        val blockingQueue = blockingQueuePool.getOrElse(messageToBeRemoved.toQueue.name,
          throw QueueDoesNotExistException(messageToBeRemoved.toQueue))
        while (!blockingQueue.isEmpty) {
          val internalMessage: InternalMessage = blockingQueue.element()
          if (System.currentTimeMillis() - internalMessage.createdTime >= messageTimeToLiveInMillis) {
            val removed = blockingQueue.remove(internalMessage)
           Log.debug(s"$removed: Removed internalMessage from it's queue ${messageToBeRemoved.toQueue}" +
                      s" because it exceeds expiration time $messageTimeToLiveInMillis millis")
          }
        }
      } catch {
        case i: InterruptedException => Log.error("Scheduled expired message cleanup was interrupted", i)
        case t: TimeoutException => Log.error(s"Expired Messages can't be cleaned due to timeout", t)
        case e: Throwable => Log.error(s"""${e.getClass.getSimpleName} "${e.getMessage}" caught by exception handler""", e)
      }
    }
  }

  object MessageScheduler {

    import java.util.concurrent.ThreadFactory

    private object LoggingUncaughtExceptionHandler extends Thread.UncaughtExceptionHandler {
      override def uncaughtException(t: Thread, e: Throwable): Unit = {
        Log.error(s"""Thread $t terminated due to ${e.getClass.getSimpleName}, "${e.getMessage}" caught by the default exception handler""", e)
      }
    }

    private class CaughtExceptionsThreadFactory extends ThreadFactory {

      override def newThread(r: Runnable): Thread = {
        val t = new Thread(r)
        t.setUncaughtExceptionHandler(LoggingUncaughtExceptionHandler)
        t
      }
    }

    private val caughtExceptionsThreadFactory: CaughtExceptionsThreadFactory = new CaughtExceptionsThreadFactory
    private val scheduler = Executors.newSingleThreadScheduledExecutor(caughtExceptionsThreadFactory)

    def scheduleMessageRedelivery(deliveryAttemptID: UUID, deliveryAttempt: DeliveryAttempt, messageRedeliveryDelay: Long, messageMaxDeliveryAttempts: Int) = {
      val messageRedeliveryRunner: MessageRedeliveryRunner = MessageRedeliveryRunner(deliveryAttemptID, deliveryAttempt, messageRedeliveryDelay, messageMaxDeliveryAttempts)
      try {
        val currentAttemptCount = deliveryAttempt.message.currentAttemptCount
        if (currentAttemptCount < messageMaxDeliveryAttempts) {
          Log.debug(s"Scheduling message redelivery attempt $currentAttemptCount, redeliver message in $messageRedeliveryDelay milliseconds.")
          val futureTask: ScheduledFuture[_] = scheduler.schedule(messageRedeliveryRunner, messageRedeliveryDelay, TimeUnit.MILLISECONDS)
          messageDeliveryAttemptMap.update(deliveryAttemptID, (deliveryAttempt, futureTask))
        } else {
          Log.debug(s"Not scheduling message redelivery because currentAttemptCount $currentAttemptCount reached max attempt number $messageMaxDeliveryAttempts.")
        }
      } catch {
        case NonFatal(x) => SchedulingMessageRedeliverySentinelProblem(messageRedeliveryDelay, x)
      }
    }

    def scheduleCleanupExpiredDeliveryAttemptInMap(queue: Queue, messageTimeToLiveInMillis: Long) = {

      // a sentinel that monitors the hashmap of messageDeliveryAttemptMap, any message that has been outstanding will be permanently removed
      // time-to-live need to get cleaned out of this map
      val cleanDeliveryAttemptRunner: CleanExpiredDeliveryAttemptRunner = CleanExpiredDeliveryAttemptRunner(queue, messageTimeToLiveInMillis)
      try {
        Log.debug(s"Starting the sentinel scheduler that cleans outstanding deliveryAttempt exceeds message expiration time: $messageTimeToLiveInMillis")
        scheduler.schedule(cleanDeliveryAttemptRunner, messageTimeToLiveInMillis, TimeUnit.MILLISECONDS)
      } catch {
        case NonFatal(x) => SchedulingCleanUpSentinelProblem(queue, messageTimeToLiveInMillis, x)
      }
    }

    def scheduleCleanupExpiredMessageInDeque(queue: Queue, messageToBeRemoved: InternalMessage, messageTimeToLiveInMillis: Long) = {
      val cleanInternalMessageInDequeRunner: CleanInternalMessageInDequeRunner = CleanInternalMessageInDequeRunner(messageToBeRemoved, messageTimeToLiveInMillis)
      try {
        Log.debug(s"Starting the sentinel scheduler that cleans outstanding internal message in" +
          s" queue ${messageToBeRemoved.toQueue} exceeds message expiration time: $messageTimeToLiveInMillis")
        scheduler.schedule(cleanInternalMessageInDequeRunner, messageTimeToLiveInMillis, TimeUnit.MILLISECONDS)
      } catch {
        case NonFatal(x) => SchedulingCleanUpSentinelProblem(queue, messageTimeToLiveInMillis, x)
      }
    }

    def scheduleMessageExpiry(to: Queue, internalMessage: InternalMessage, messageTimeToLiveInMillis: Long) = {
      // schedule future cleanup of deliveryAttempts when the message expires
      MessageScheduler.scheduleCleanupExpiredDeliveryAttemptInMap(to, messageTimeToLiveInMillis)
      // schedule future cleanup of this message in blockingQueue if client never call receive
      MessageScheduler.scheduleCleanupExpiredMessageInDeque(to, internalMessage, messageTimeToLiveInMillis)
    }

    def cancelScheduledMessageRedelivery(futureTask: ScheduledFuture[_]): Unit = {
      // returns false if the task could not be cancelled, typically because it has already completed normally;
      futureTask.cancel(true)
    }

    def shutDown(): util.List[Runnable] = {
      scheduler.shutdownNow()
    }
  }
}

case class InternalMessage(id: UUID, contents: String, createdTime: Long, toQueue: Queue, currentAttemptCount: Int) {
  override def equals(obj: scala.Any): Boolean = {
    obj match {
      case other: InternalMessage =>
        other.canEqual(this) && other.id == this.id
      case _ => false
    }
  }
}

case class DeliveryAttempt(message: InternalMessage, createdTime: Long, fromQueue: Queue)

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

  override def summary: String = s"""The Hub encountered an exception while trying to
                                    |cleanup messages that has been outstanding for more
                                    |than $timeOutInMillis milliseconds in queue ${queue.name}. """.stripMargin

  override def description: String = s"""The Hub encountered an exception while trying
                                        |to cleanup messages that has been outstanding
                                        |for more than $timeOutInMillis milliseconds in queue ${queue.name}
                                        |on Thread ${Thread.currentThread().getName}: ${x.getMessage}""".stripMargin
}

case class SchedulingCleanUpSentinelProblem(queue: Queue, timeOutInMillis: Long, x:Throwable) extends AbstractProblem(ProblemSources.Hub) {
  override val throwable = Some(x)

  override def summary: String = s"""The Hub encountered an exception while trying to
                                    |schedule a sentinel that cleans up outstanding messages
                                    |exceed $timeOutInMillis milliseconds in queue ${queue.name}.""".stripMargin

  override def description: String = s"""The Hub encountered an exception while trying to
                                        |schedule a sentinel that cleans up outstanding messages
                                        |exceed $timeOutInMillis milliseconds  in queue ${queue.name}
                                        |on Thread ${Thread.currentThread().getName}: ${x.getMessage}""".stripMargin
}

case class SchedulingMessageRedeliverySentinelProblem(messageRedeliveryDelay: Long, x:Throwable) extends AbstractProblem(ProblemSources.Hub) {
  override val throwable = Some(x)

  override def summary: String = s"""The Hub encountered an exception while trying to
                                    |schedule a sentinel that redelivers an incomplete message after $messageRedeliveryDelay milliseconds""".stripMargin

  override def description: String = s"""The Hub encountered an exception while trying to
                                        |schedule a sentinel that redelivers an incomplete message after $messageRedeliveryDelay
                                        |milliseconds on Thread ${Thread.currentThread().getName}: ${x.getMessage}""".stripMargin
}

case class QueueDoesNotExistException(queueName: Queue) extends Exception(
  s"""Cannot match given ${queueName.name} to any Queue in the server!
     |Queue does not exist!""".stripMargin)

case class MessageDoesNotExistAndCannotBeCompletedException(id: UUID) extends Exception(
  s"""Message does not exist and cannot be completed!
     |Message might have already been completed or expired!""".stripMargin)
