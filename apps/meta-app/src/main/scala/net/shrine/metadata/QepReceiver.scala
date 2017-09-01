package net.shrine.metadata

import com.typesafe.config.Config
import net.shrine.config.ConfigExtensions
import net.shrine.hornetqclient.CouldNotCreateQueueButOKToRetryException
import net.shrine.log.Log
import net.shrine.messagequeueservice.{Message, MessageQueueService, Queue}
import net.shrine.problem.ProblemNotYetEncoded
import net.shrine.protocol.{ResultOutputType, ResultOutputTypes, RunQueryResponse}
import net.shrine.qep.querydb.QepQueryDb
import net.shrine.source.ConfigSource

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
          Log.debug("Successfully called receive.")
        } catch {
          case NonFatal(x) => Log.error("Exception while receiving a message.", x) //todo new kind of problem
          //pass-through to blow up the thread, receive no more results, do something dramatic in UncaughtExceptionHandler.
          case x => Log.error("Fatal exception while receiving a message", x)
            throw x
        }
      }
    }

    def receiveAMessage(queue:Queue): Unit = {
      val maybeMessage: Try[Option[Message]] = MessageQueueService.service.receive(queue, pollDuration) //todo make this configurable (and testable)

      maybeMessage.transform({m =>
        m.foreach(interpretAMessage(_,queue))
        Success(m)
      },{x =>
        ProblemNotYetEncoded(s"Something went wrong during receive from $queue",x) //todo create full-up problem
        Failure(x)
      })
    }

    val unit = ()
    def interpretAMessage(message: Message,queue: Queue): Unit = {
      Log.debug(s"Received a message from $queue of $message")

      val xmlString = message.contents
      val rqrt: Try[RunQueryResponse] = RunQueryResponse.fromXmlString(breakdownTypes)(xmlString)
      rqrt.transform({ rqr =>
        Log.debug(s"Inserting result from ${rqr.singleNodeResult.description} for query ${rqr.queryId}")
        QepQueryDb.db.insertQueryResult(rqr.queryId, rqr.singleNodeResult)
        message.complete()
        Success(unit)
      },{ x =>
        x match {
          case NonFatal(nfx) => Log.error(s"Could not decode message $xmlString ",x)
          case _ =>
        }
        Failure(x)
      }).get
    }

    def createQueue(nodeName:String):Queue = {

      //Either come back with the right exception to try again, or a Queue
      def tryToCreateQueue():Try[Queue] =
        MessageQueueService.service.createQueueIfAbsent(nodeName)

      def keepGoing(attempt:Try[Queue]):Try[Boolean] = attempt.transform({queue => Success(false)}, {
        case okIsh: CouldNotCreateQueueButOKToRetryException => Success(true)
        case x => Failure(x)
      })

      //todo for fun figure out how to do this without the var. maybe a Stream ?
      var lastAttempt:Try[Queue] = tryToCreateQueue()
      while(keepGoing(lastAttempt).get) {
        Log.debug(s"Last attempt to create a queue resulted in ${lastAttempt}. Sleeping $pollDuration before next attempt")
        Thread.sleep(pollDuration.toMillis)
        lastAttempt = tryToCreateQueue()
      }
      Log.info(s"Finishing createQueue with $lastAttempt")

      lastAttempt.get
    }
  }
}