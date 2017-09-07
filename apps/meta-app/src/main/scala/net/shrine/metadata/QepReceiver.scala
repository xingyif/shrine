package net.shrine.metadata

import com.typesafe.config.Config
import net.shrine.config.ConfigExtensions
import net.shrine.hornetqclient.CouldNotCreateQueueButOKToRetryException
import net.shrine.log.Log
import net.shrine.messagequeueservice.protocol.Envelope
import net.shrine.messagequeueservice.{Message, MessageQueueService, Queue}
import net.shrine.problem.{AbstractProblem, ProblemSources}
import net.shrine.protocol.{AggregatedRunQueryResponse, ResultOutputType, ResultOutputTypes}
import net.shrine.qep.querydb.QepQueryDb
import net.shrine.source.ConfigSource
import net.shrine.util.Versions

import scala.concurrent.duration.Duration
import scala.util.{Failure, Success, Try}
import scala.util.control.NonFatal

/**
  * Receives messages and writes the result to the QEP's cache
  *
  * @author david
  * @since 8/18/17
  */
object QepReceiver {

  val config: Config = ConfigSource.config
  val nodeName = config.getString("shrine.humanReadableNodeName")

  //create a daemon thread that long-polls for messages forever
  val pollingThread = new Thread(QepReceiverRunner(nodeName),s"${getClass.getSimpleName} poller")
  pollingThread.setDaemon(true)
  //todo   pollingThread.setUncaughtExceptionHandler() SHRINE-2198

  pollingThread.start()
  Log.debug(s"Started the QepReceiver thread for $nodeName")

  case class QepReceiverRunner(nodeName:String) extends Runnable {

    val pollDuration = Duration("15 seconds") //todo from config

    val breakdownTypes: Set[ResultOutputType] = ConfigSource.config.getOptionConfigured("shrine.breakdownResultOutputTypes", ResultOutputTypes.fromConfig).getOrElse(Set.empty)

    override def run(): Unit = {
      val queue = createQueue(nodeName)

      while (true) {
        //forever
        try {
          //todo only ask to receive a message if there are incomplete queries SHRINE-2196
          Log.debug("About to call receive.")
          receiveAMessage(queue)
          Log.debug("Called receive.")
        } catch {
          case NonFatal(x) => ExceptionWhileReceivingMessage(queue,x)
          //pass-through to blow up the thread, receive no more results, do something dramatic in UncaughtExceptionHandler.
          case x => Log.error("Fatal exception while receiving a message", x)
            throw x
        }
      }
    }

    def receiveAMessage(queue:Queue): Unit = {
      val maybeMessage: Try[Option[Message]] = MessageQueueService.service.receive(queue, pollDuration) //todo make pollDuration configurable (and testable)

      maybeMessage.transform({m =>
        m.map(interpretAMessage(_,queue)).getOrElse(Success())
      },{x =>
        x match {
          case NonFatal(nfx) => ExceptionWhileReceivingMessage(queue,x)
          case _ => //pass through
        }
        Failure(x)
      })
    }

    def interpretAMessage(message: Message,queue: Queue): Try[Unit] = {
      val unit = ()
      Log.debug(s"Received a message from $queue of $message")

      val contents = message.contents

      Envelope.fromJson(contents).flatMap{
        case e:Envelope if e.shrineVersion == Versions.version => Success(e)
        case e:Envelope => Failure(new IllegalArgumentException(s"Envelope version is not ${Versions.version}")) //todo better exception
        case notE => Failure(new IllegalArgumentException(s"Not an expected message Envelope but a ${notE.getClass}")) //todo better exception
      }.flatMap {
        case Envelope(contentsType, contents, shrineVersion) if contentsType == AggregatedRunQueryResponse.getClass.getSimpleName => {
          AggregatedRunQueryResponse.fromXmlString(breakdownTypes)(contents)
        }
        case _ => Failure(new IllegalArgumentException("Not an expected type of message from this queue")) //todo better exception
      }.transform({ rqr =>
        QepQueryDb.db.insertQueryResult(rqr.queryId, rqr.results.head)
        Log.debug(s"Inserted result from ${rqr.results.head.description} for query ${rqr.queryId}")
        message.complete()
        Success(unit)
      },{ x =>
        x match {
          case NonFatal(nfx) => QepReceiverCouldNotDecodeMessage(contents,queue,x)
          case _ => //pass through
        }
        Failure(x)
      })
    }

    def createQueue(nodeName:String):Queue = {

      //Either come back with the right exception to try again, or a Queue
      def tryToCreateQueue():Try[Queue] =
        MessageQueueService.service.createQueueIfAbsent(nodeName)

      def keepGoing(attempt:Try[Queue]):Try[Boolean] = attempt.transform({queue => Success(false)}, {
        case okIsh: CouldNotCreateQueueButOKToRetryException => Success(true)
        case x => Failure(x)
      })

      //todo for fun figure out how to do this without the var. maybe a Stream ? SHRINE-2211
      var lastAttempt:Try[Queue] = tryToCreateQueue()
      while(keepGoing(lastAttempt).get) {
        Log.debug(s"Last attempt to create a queue resulted in $lastAttempt. Sleeping $pollDuration before next attempt")
        Thread.sleep(pollDuration.toMillis)
        lastAttempt = tryToCreateQueue()
      }
      Log.info(s"Finishing createQueue with $lastAttempt")

      lastAttempt.get
    }
  }
}

case class ExceptionWhileReceivingMessage(queue:Queue, x:Throwable) extends AbstractProblem(ProblemSources.Qep) {

  override val throwable = Some(x)

  override def summary: String = s"The QEP encountered an exception while trying to receive a message from $queue"

  override def description: String = s"The QEP encountered an exception while trying to receive a message from $queue on ${Thread.currentThread().getName}: ${x.getMessage}"
}

case class QepReceiverCouldNotDecodeMessage(messageString:String,queue:Queue, x:Throwable) extends AbstractProblem(ProblemSources.Qep) {

  override val throwable = Some(x)

  override def summary: String = s"The QEP could not decode a message from $queue"

  override def description: String =
    s"""The QEP encountered an exception while trying to decode a message from $queue on ${Thread.currentThread().getName}:
       |${x.getMessage}
       |$messageString""".stripMargin
}