package net.shrine.messagequeuemiddleware

import java.util
import java.util.concurrent.{BlockingDeque, Executors, LinkedBlockingDeque, ScheduledFuture, TimeUnit, TimeoutException}
import java.util.{Objects, UUID}

import net.shrine.config.ConfigExtensions
import net.shrine.log.Log
import net.shrine.messagequeueservice.{Message, MessageQueueService, Queue}
import net.shrine.problem.{AbstractProblem, ProblemSources}
import net.shrine.source.ConfigSource
import org.json4s.ShortTypeHints
import org.json4s.native.Serialization
import scala.util.Success
import scala.util.Failure

import scala.collection.concurrent.{TrieMap, Map => ConcurrentMap}
import scala.collection.immutable.Seq
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Future, Promise, blocking}
import scala.util.Try
import scala.util.control.NonFatal

/**
  * This object is the local version of the Message-Oriented Middleware API, which uses MessageQueue service
  *
  * @author david, Yifan
  * @since 7/18/17
  */

object LocalMessageQueueMiddleware extends MessageQueueService {

  val configPath = "shrine.messagequeue.blockingq"

  def config = ConfigSource.config.getConfig(configPath)

  private def messageTimeToLiveInMillis: Long = config.get("messageTimeToLive", Duration(_)).toMillis

  private def messageRedeliveryDelay: Long = config.get("messageRedeliveryDelay", Duration(_)).toMillis

  private def messageMaxDeliveryAttempts: Int = config.getInt("messageMaxDeliveryAttempts")

  // keep a map of messages and ids
  private val messageDeliveryAttemptMap: TrieMap[UUID, (DeliveryAttempt, Option[ScheduledFuture[_]])] = TrieMap.empty

  /**
    * Use LocalMessageQueueStopper to stop the MessageQueueMiddleware without unintentionally starting it
    */
  private[messagequeuemiddleware] def stop() = {
    MessageScheduler.shutDown()
  }

  //todo key should be a Queue instead of a String SHRINE-2308
  //todo rename
  val blockingQueuePool: ConcurrentMap[String, BlockingDeque[InternalMessage]] = TrieMap.empty

  val receiveMessagePromisesMap: ConcurrentMap[Queue, BlockingDeque[(Promise[Unit], Long)]] = TrieMap.empty

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
  def send(contents: String, to: Queue): Future[Unit] = Future {
    val queue = blockingQueuePool.getOrElse(to.name, throw QueueDoesNotExistException(to))
    // creates a message
    val msgID: UUID = UUID.randomUUID()
    val internalMessage: InternalMessage = InternalMessage(msgID, contents, System.currentTimeMillis(), to, 0)
    // schedule future cleanup when the message expires
    MessageScheduler.scheduleExpiredMessageCleanup(to, internalMessage, messageTimeToLiveInMillis)
    // waiting if necessary for space to become available
    blocking(queue.putLast(internalMessage))
    // complete the receive promise of the message, if client is currently waiting
    receiveMessagePromisesMap.get(to).fold(Log.debug(s"No receiveMessage promise to fulfill for $to"))({ blockingDeque: BlockingDeque[(Promise[Unit], Long)] =>
      Option(blockingDeque.poll()).fold(Log.debug(s"No receiveMessage promise to fulfill for $to"))({ promiseAndDeadline: (Promise[Unit], Long) =>
        if (promiseAndDeadline._2 > System.currentTimeMillis()) {
          // promise is not expired yet
          if (promiseAndDeadline._1.trySuccess(Unit)) Log.debug(s"Successfully completed one receiveMessage promise for $to")
        } else {
          Log.debug(s"One receiveMessage promise for $to found. No receiveMessage promise completed, because the promise exceeds receiveMessage deadline ${promiseAndDeadline._2}. Deleted the expired promise.")
        }
      })
    })
    Log.debug(s"After send to ${to.name} - blockingQueue ${queue.size} $queue")
  }

  //receive a message
  /**
    * Always do AWS SQS-style long polling.
    * Be sure your code can handle receiving the same message twice.
    *
    * Use Promise and Scheduler
    * Message received immediately: fulfill promise
    * Message received within timeout: fulfill promise
    * Message not received after timeout: fulfill with None
    *
    * @return Some(message) before the timeout, or None
    *
    */
  def receive(from: Queue, timeout: Duration): Future[Option[Message]] = {
    val deadline: Long = System.currentTimeMillis() + timeout.toMillis

    // ues Promises if no message available at first
    val promiseResponse: Promise[Option[SimpleMessage]] = Promise[Option[SimpleMessage]]()
    val blockingQueueTry: Try[BlockingDeque[InternalMessage]] = Try {
      blockingQueuePool.getOrElse(from.name, throw QueueDoesNotExistException(from))
    }.transform({ blockingQueue: BlockingDeque[InternalMessage] =>
      Log.debug(s"Before receive from ${from.name} - blockingQueue ${blockingQueue.size} ${blockingQueue.toString}")
      Success(blockingQueue)
    }, { throwable: Throwable =>
      throwable match {
        case NonFatal(t) => {
          // complete the promise with a failure if queue not found or other exceptions
          Log.error(s"Failed to receive from $from due to $t")
          promiseResponse.tryFailure(throwable) // QueueDoesNotExistException
          Failure(t)
        }
      }
    })

    def internalMessageToSimpleMessageAndRedeliver(internalMessage: InternalMessage): SimpleMessage = {
      val deliveryAttemptID = UUID.randomUUID()
      // message available when first call receive
      val deliveryAttempt: DeliveryAttempt = DeliveryAttempt(internalMessage, internalMessage.createdTime, from)
      MessageScheduler.scheduleMessageRedelivery(deliveryAttemptID, deliveryAttempt, messageRedeliveryDelay, messageMaxDeliveryAttempts)
      SimpleMessage(deliveryAttemptID.toString, internalMessage.contents)
    }

    ///////////////// try to poll the first message from the blocking deque /////////////////////////////
    blockingQueueTry.map({ blockingQueue: BlockingDeque[InternalMessage] =>
      Option(blockingQueue.poll()).map({ internalMessage: InternalMessage =>
        promiseResponse.trySuccess(Some(internalMessageToSimpleMessageAndRedeliver(internalMessage)))
      }).getOrElse({
        ////////////// not able to receive message on the first receive try ////////////////////////
        ///////////// schedule time out if no message received after timeout //////////////////////
        val promiseTimeout: Promise[Unit] = Promise[Unit]()
        promiseTimeout.future.transform({ unit: Unit =>
          // try receive again before time out
          promiseResponse.tryComplete(Try {
            Option(blockingQueue.poll()).map({ internalMessage: InternalMessage =>
              Some(internalMessageToSimpleMessageAndRedeliver(internalMessage))
            }).getOrElse({
              // No message available from the queue
              Log.debug(s"No message available from the queue ${from.name} after waiting for $timeout")
              None
            })
          })
        }, { throwable: Throwable =>
          throwable match {
            case NonFatal(t) => ExceptionWhilePreparingTimeoutResponse("receiving message", from, timeout, t)
          }
          throwable
        })

        val timeLeft: Long = deadline - System.currentTimeMillis()
        case class FulfillPromiseRunner(promise: Promise[Unit]) extends Runnable {
          val unit: Unit = ()

          override def run(): Unit = promise.trySuccess(unit)
        }
        val scheduledTaskToFulfillPromiseWithNone: ScheduledFuture[_] = MessageScheduler.scheduleOnce(timeLeft, FulfillPromiseRunner(promiseTimeout))

        //////////////////////// message comes back within timeout /////////////////////////
        val receiveMessagePromiseWithinTimeOut = Promise[Unit]()
        // put promise in map, complete when send
        val dequeOfPromiseAndTimeleft = new LinkedBlockingDeque[(Promise[Unit], Long)]()
        dequeOfPromiseAndTimeleft.addFirst(receiveMessagePromiseWithinTimeOut, deadline)
        receiveMessagePromisesMap.getOrElseUpdate(from, dequeOfPromiseAndTimeleft).putLast((receiveMessagePromiseWithinTimeOut, timeLeft))
        // schedule cleaning up the expired promises after deadline
        MessageScheduler.scheduleExpiredPromisesCleanUp(from, timeLeft)

        // when the promise is completed (received message before time out), complete the original promise
        receiveMessagePromiseWithinTimeOut.future.transform({ unit =>
          Log.debug(s"Checking for new message from Queue $from")

          Option(blockingQueue.poll()).map({ internalMessage: InternalMessage =>
            Log.debug(s"Received a new message from Queue $from, within timeout $timeout")
            scheduledTaskToFulfillPromiseWithNone.cancel(true)
            promiseResponse.trySuccess(Some(internalMessageToSimpleMessageAndRedeliver(internalMessage)))
          })

        }, { throwable: Throwable =>
          throwable match {
            case NonFatal(t) => ExceptionWhilePreparingTimeoutResponse("receiving message", from, timeout, t)
          }
          throwable
        })
      })
    })
    promiseResponse.future
  }


  def completeMessage(deliveryAttemptID: UUID): Future[Unit] = Future {
    val deliveryAttemptAndFutureTaskOpt: Option[(DeliveryAttempt, Option[ScheduledFuture[_]])] = messageDeliveryAttemptMap.get(deliveryAttemptID)

    deliveryAttemptAndFutureTaskOpt.fold(
      // if message delivery attempt does not exist in the map, then it might be in the queue or expired
      throw MessageDoesNotExistAndCannotBeCompletedException(deliveryAttemptID)
    ) { (deliveryAttemptAndFutureTask: (DeliveryAttempt, Option[ScheduledFuture[_]])) =>
      val deliveryAttempt: DeliveryAttempt = deliveryAttemptAndFutureTask._1
      val internalToBeSentMessage: InternalMessage = deliveryAttempt.message
      val queue: Queue = internalToBeSentMessage.toQueue
      // removes all deliveryAttempts of the message from the map and cancels all the scheduled redelivers
      for ((uuid: UUID, eachDAandTask: (DeliveryAttempt, Option[ScheduledFuture[_]])) <- messageDeliveryAttemptMap) {
        // internalMessage changes when it is redelivered, but id remains the same
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
    }
  }

  //todo dead letter queue for all messages SHRINE-2261

  case class SimpleMessage(deliveryAttemptID: String, content: String) extends Message {
    def toJson: String = {
      Serialization.write(this)(SimpleMessage.messageFormats)
    }

    override def complete(): Future[Unit] = Future {
      val uuid: UUID = UUID.fromString(deliveryAttemptID)
      LocalMessageQueueMiddleware.completeMessage(uuid)
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

  case class MessageRedeliveryRunner(deliveryAttemptID: UUID, deliveryAttempt: DeliveryAttempt, messageMaxDeliveryAttempts: Long) extends Runnable {
    override def run(): Unit = {
      try {
        messageDeliveryAttemptMap.get(deliveryAttemptID).fold(
          Log.debug(s"Could not find deliveryAttempt for message ${deliveryAttempt.message.contents} from queue ${deliveryAttempt.fromQueue}")
        ) { (deliveryAttemptAndFutureTask: (DeliveryAttempt, Option[ScheduledFuture[_]])) =>
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

  case class CleanDeliveryAttemptandInternalMessageRunner(queue: Queue, messageToBeRemoved: InternalMessage, messageTimeToLiveInMillis: Long) extends Runnable {
    override def run(): Unit = {
      val currentTime: Long = System.currentTimeMillis()
      try {
        Log.debug(s"About to clean up outstanding messages. DAMap size: ${messageDeliveryAttemptMap.size}")
        // cleans up deliveryAttempt map
        for ((uuid: UUID, deliveryAttemptAndFutureTask: (DeliveryAttempt, Option[ScheduledFuture[_]])) <- messageDeliveryAttemptMap) {
          if ((currentTime - deliveryAttemptAndFutureTask._1.message.createdTime) >= messageTimeToLiveInMillis) {
            messageDeliveryAttemptMap.remove(uuid, deliveryAttemptAndFutureTask)
            // cancels its future message redelivery task
            MessageScheduler.cancelScheduledMessageRedelivery(deliveryAttemptAndFutureTask._2)
          }
        }
        Log.debug(s"Outstanding deliveryAttempts that exceed $messageTimeToLiveInMillis milliseconds have been cleaned from the map. " +
          s"DAMap size: ${messageDeliveryAttemptMap.size}")

        val blockingQueue = blockingQueuePool.getOrElse(messageToBeRemoved.toQueue.name,
          throw QueueDoesNotExistException(messageToBeRemoved.toQueue))
        while (!blockingQueue.isEmpty) {
          val internalMessage: InternalMessage = blockingQueue.element()
          if (currentTime - internalMessage.createdTime >= messageTimeToLiveInMillis) {
            val removed = blockingQueue.remove(internalMessage)
            Log.debug(s"$removed: Removed internalMessage from it's queue ${messageToBeRemoved.toQueue}" +
              s" because it exceeds expiration time $messageTimeToLiveInMillis millis")
          }
        }
      } catch {
        case NonFatal(x) => CleaningUpDeliveryAttemptandInternalMessageProblem(queue, messageTimeToLiveInMillis, x)
        case i: InterruptedException => Log.error("Scheduled expired message cleanup was interrupted", i)
        case t: TimeoutException => Log.error(s"Expired Messages can't be cleaned due to timeout", t)
        case e: Throwable => Log.error(s"""${e.getClass.getSimpleName} "${e.getMessage}" caught by exception handler""", e)
      }
    }
  }

  case class CleanExpiredPromisesRunner(queue: Queue, timeToCleanUp: Long) extends Runnable {
    override def run(): Unit = {
      try {
        receiveMessagePromisesMap.values.foreach({ blockingDeque: BlockingDeque[(Promise[Unit], Long)] =>
          val iterator = blockingDeque.iterator()
          while (iterator.hasNext) {
            val next: (Promise[Unit], Long) = iterator.next()
            val promise: Promise[Unit] = next._1 // we don't need to fulfill an expired promise
            val promiseDeadline: Long = next._2
            if (promiseDeadline < timeToCleanUp) {
              // promise expired, remove from map
              blockingDeque.remove(next)
              Log.debug(s"Removed expired promise for $queue, deadline $timeToCleanUp exceeded.")
            }
          }
        })
      } catch {
        case i: InterruptedException => Log.error("Scheduled expired promise clean up was interrupted", i)
        case t: TimeoutException => Log.error(s"Expired promise can't be removed due to timeout", t)
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

    def scheduleMessageRedelivery(deliveryAttemptID: UUID, deliveryAttempt: DeliveryAttempt, messageRedeliveryDelay: Long, messageMaxDeliveryAttempts: Int): Unit = {
      val messageRedeliveryRunner: MessageRedeliveryRunner = MessageRedeliveryRunner(deliveryAttemptID, deliveryAttempt, messageMaxDeliveryAttempts)
      try {
        val currentAttemptCount = deliveryAttempt.message.currentAttemptCount
        if (currentAttemptCount < messageMaxDeliveryAttempts) {
          Log.debug(s"Scheduling message redelivery attempt $currentAttemptCount, redeliver message in $messageRedeliveryDelay milliseconds.")
          val futureTask: ScheduledFuture[_] = scheduler.schedule(messageRedeliveryRunner, messageRedeliveryDelay, TimeUnit.MILLISECONDS)
          // update the DAMap with new DAID, DA, and scheduled future redelivery task
          messageDeliveryAttemptMap.update(deliveryAttemptID, (deliveryAttempt, Some(futureTask)))
        } else {
          Log.debug(s"Not scheduling message redelivery because currentAttemptCount $currentAttemptCount reached max attempt number $messageMaxDeliveryAttempts.")
          // update the DAMap with new DAID, DA, and None (no scheduled future redelivery task)
          messageDeliveryAttemptMap.update(deliveryAttemptID, (deliveryAttempt, None))
        }
      } catch {
        case NonFatal(x) => SchedulingMessageRedeliverySentinelProblem(messageRedeliveryDelay, x)
      }
    }

    def scheduleExpiredMessageCleanup(queue: Queue, messageToBeRemoved: InternalMessage, messageTimeToLiveInMillis: Long): Unit = {
      val deadline = System.currentTimeMillis() + messageTimeToLiveInMillis
      val cleanDeliveryAttemptandInternalMessageRunner: CleanDeliveryAttemptandInternalMessageRunner = CleanDeliveryAttemptandInternalMessageRunner(queue, messageToBeRemoved, deadline - System.currentTimeMillis())
      try {
        Log.debug(s"Starting the sentinel scheduler that cleans outstanding internal message in" +
          s" queue ${messageToBeRemoved.toQueue} exceeds message expiration time: $messageTimeToLiveInMillis")
        scheduler.schedule(cleanDeliveryAttemptandInternalMessageRunner, deadline - System.currentTimeMillis(), TimeUnit.MILLISECONDS)
      } catch {
        case NonFatal(x) => SchedulingCleanUpSentinelProblem(queue, "messages", messageTimeToLiveInMillis, x)
      }
    }


    def scheduleExpiredPromisesCleanUp(queue: Queue, timeLeft: Long): Unit = {
      val timeToCleanUp = System.currentTimeMillis() + timeLeft
      val cleanExpiredPromisesRunner: CleanExpiredPromisesRunner = CleanExpiredPromisesRunner(queue, timeToCleanUp)
      try {
        Log.debug(s"Starting the sentinel scheduler that cleans expired promises for $queue, exceeds deadline: $timeLeft")
        scheduler.schedule(cleanExpiredPromisesRunner, timeToCleanUp - System.currentTimeMillis(), TimeUnit.MILLISECONDS)
      } catch {
        case NonFatal(x) => SchedulingCleanUpSentinelProblem(queue, "promises", timeLeft, x)
      }

    }



    def scheduleOnce(timeLeft: Long, runnable: Runnable) = {
      scheduler.schedule(runnable, timeLeft, TimeUnit.MILLISECONDS)
    }


    def cancelScheduledMessageRedelivery(futureTask: Option[ScheduledFuture[_]]): Unit = {
      // returns false if the task could not be cancelled, typically because it has already completed normally;
      futureTask.fold(Log.info("No scheduled future task to cancel"))(f => f.cancel(true))
    }

    def shutDown(): util.List[Runnable] = {
      scheduler.shutdownNow()
    }
  }

}

case class InternalMessage(id: UUID, contents: String, createdTime: Long, toQueue: Queue, currentAttemptCount: Int) {
  // internalMessage changes when it is redelivered, InternalMessage s with different currentAttemptCounts can be equal.
  // because we no longer use atomicInteger and each time we create a new InternalMessage to increment currentAttemptCount
  override def equals(obj: scala.Any): Boolean = {
    obj match {
      case other: InternalMessage =>
        other.canEqual(this) && other.id == this.id
      case _ => false
    }
  }

  override def hashCode(): Int = {
    Objects.hash(id, contents, createdTime.toString, toQueue)
  }
}

case class DeliveryAttempt(message: InternalMessage, createdTime: Long, fromQueue: Queue)

/**
  * If the configuration is such that MessageQueue should have been started use this object to stop it
  */
object LocalMessageQueueStopper {

  def stop(): Unit = {
    //a lot less interesting without MessageQueue - not a big deal to stop schedulers that were never started , maybe nothing to do.
    LocalMessageQueueMiddleware.stop()
  }

}

case class CleaningUpDeliveryAttemptandInternalMessageProblem(queue: Queue, timeOutInMillis: Long, x: Throwable) extends AbstractProblem(ProblemSources.Hub) {

  override val throwable = Some(x)

  override def summary: String =
    s"""The Hub encountered an exception while trying to
        |cleanup messages that has been outstanding for more
        |than $timeOutInMillis milliseconds in queue ${queue.name}. """.stripMargin

  override def description: String =
    s"""The Hub encountered an exception while trying
        |to cleanup messages that has been outstanding
        |for more than $timeOutInMillis milliseconds in queue ${queue.name}
        |on Thread ${Thread.currentThread().getName}: ${x.getMessage}""".stripMargin
}

case class SchedulingCleanUpSentinelProblem(queue: Queue, toBeCleaned: String, timeOutInMillis: Long, x: Throwable) extends AbstractProblem(ProblemSources.Hub) {
  override val throwable = Some(x)

  override def summary: String =
    s"""The Hub encountered an exception while trying to
        |schedule a sentinel that cleans up outstanding $toBeCleaned
        |exceed $timeOutInMillis milliseconds in queue ${queue.name}.""".stripMargin

  override def description: String =
    s"""The Hub encountered an exception while trying to
        |schedule a sentinel that cleans up outstanding $toBeCleaned
        |exceed $timeOutInMillis milliseconds  in queue ${queue.name}
        |on Thread ${Thread.currentThread().getName}: ${x.getMessage}""".stripMargin
}

case class SchedulingMessageRedeliverySentinelProblem(messageRedeliveryDelay: Long, x: Throwable) extends AbstractProblem(ProblemSources.Hub) {
  override val throwable = Some(x)

  override def summary: String =
    s"""The Hub encountered an exception while trying to
        |schedule a sentinel that redelivers an incomplete message after $messageRedeliveryDelay milliseconds""".stripMargin

  override def description: String =
    s"""The Hub encountered an exception while trying to
        |schedule a sentinel that redelivers an incomplete message after $messageRedeliveryDelay
        |milliseconds on Thread ${Thread.currentThread().getName}: ${x.getMessage}""".stripMargin
}

case class QueueDoesNotExistException(queueName: Queue) extends Exception(
  s"""Cannot match given ${queueName.name} to any Queue in the server!
      |Queue does not exist!""".stripMargin)

case class MessageDoesNotExistAndCannotBeCompletedException(id: UUID) extends Exception(
  s"""Message does not exist and cannot be completed!
      |Message might have already been completed or expired!""".stripMargin)

case class ExceptionWhilePreparingTimeoutResponse(task: String, queue: Queue, timeout: Duration, t: Throwable) extends AbstractProblem(ProblemSources.Hub) {

  override def throwable = Some(t)

  override def summary: String = "Unable to prepare a triggered response due to an exception."

  override def description: String = s"Unable to prepare a promised response for $task to Queue $queue within timeout $timeout due to a ${t.getClass.getSimpleName}"
}