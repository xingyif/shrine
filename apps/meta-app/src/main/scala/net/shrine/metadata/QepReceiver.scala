package net.shrine.metadata

import java.util.concurrent.atomic.AtomicBoolean
import javax.servlet.{ServletContextEvent, ServletContextListener}

import com.typesafe.config.Config
import net.shrine.broadcaster.{IdAndUrl, NodeListParser}
import net.shrine.config.ConfigExtensions
import net.shrine.log.Log
import net.shrine.messagequeueservice.{CouldNotCompleteMomTaskButOKToRetryException, Message, MessageQueueService, Queue}
import net.shrine.problem.{AbstractProblem, ProblemSources}
import net.shrine.protocol.version.v24.{Envelope, IncrementalQueryResult}
import net.shrine.protocol.{AggregatedRunQueryResponse, QueryResult, ResultOutputType, ResultOutputTypes}
import net.shrine.qep.querydb.{QepQueryDb, QepQueryDbChangeNotifier, QueryResultRow}
import net.shrine.source.ConfigSource

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}

/**
  * Receives messages and writes the result to the QEP's cache
  *
  * @author david
  * @since 8/18/17
  */
object QepReceiver {

  val config: Config = ConfigSource.config
  val nodeName = config.getString("shrine.humanReadableNodeName")
  val pollDuration = config.get("shrine.messagequeue.receiveWaitTime",Duration(_))


  //create a daemon thread that long-polls for messages forever
  val runner = QepReceiverRunner(nodeName,pollDuration)

  val pollingThread = new Thread(runner,s"${getClass.getSimpleName} poller")
  pollingThread.setDaemon(true)
  pollingThread.setUncaughtExceptionHandler(QepReceiverUncaughtExceptionHandler)

  def start(): Unit = {

    pollingThread.start()
    Log.debug(s"Started the QepReceiver thread for $nodeName")
  }

  def stop(): Unit = {
    runner.stop()
  }

  case class QepReceiverRunner(nodeName:String,pollDuration:Duration) extends Runnable {

    val keepGoing = new AtomicBoolean(true)

    def stop(): Unit = {
      keepGoing.set(false)
      Log.debug(s"${this.getClass.getSimpleName} keepGoing set to ${keepGoing.get()}. Will stop asking for messages after the current request.")
    }

    val breakdownTypes: Set[ResultOutputType] = ConfigSource.config.getOptionConfigured("shrine.breakdownResultOutputTypes", ResultOutputTypes.fromConfig).getOrElse(Set.empty)

    override def run(): Unit = {

      //if hub, create all the queues
      if(config.getBoolean("shrine.hub.create")) {
        val otherNodes: List[IdAndUrl] = config.getOptionConfigured("shrine.hub.downstreamNodes", NodeListParser(_)).getOrElse(Nil).to[List]
        val thisNode:Option[String] = if (config.getBoolean("shrine.hub.shouldQuerySelf")) Some(nodeName)
        else None

        val nodeNames = ( thisNode :: otherNodes.map(n => Some(n.nodeId.name)) ).flatten
        nodeNames.foreach(createQueue)
      }

      val queue = createQueue(nodeName)

      while (keepGoing.get()) {
        //forever
        try {
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
      Log.debug(s"QepReceiverRunner will stop. keepGoing is ${keepGoing.get()}")
    }

    def receiveAMessage(queue:Queue): Unit = {
      val messageFuture: Future[Option[Message]] = MessageQueueService.service.receive(queue, pollDuration)

      messageFuture.transform({m =>
        m.map(interpretAMessage(_,queue)).getOrElse(Success()) //todo rework this in SHRINE-2327
      },{x =>
        x match {
          case cncmtbotrx:CouldNotCompleteMomTaskButOKToRetryException => {
            Log.debug(s"Last attempt to receive resulted in ${cncmtbotrx.getMessage}. Sleeping $pollDuration before next attempt",cncmtbotrx)
            Thread.sleep(pollDuration.toMillis)
          }
          case NonFatal(nfx) => ExceptionWhileReceivingMessage(queue,x)
          case fatal => throw fatal
        }
        x
      })
    }

    def interpretAMessage(message: Message,queue: Queue): Try[Unit] = {
      val unit = ()
      Log.debug(s"Received a message from $queue of $message")

      val envelopeJson = message.contents

      Envelope.fromJson(envelopeJson).
        flatMap{
        case e:Envelope => e.checkVersionExactMatch
        case notE => Failure(new IllegalArgumentException(s"Not an expected message Envelope but a ${notE.getClass}"))
      }.
        flatMap {
        case Envelope(contentsType, contents, shrineVersion) if contentsType == AggregatedRunQueryResponse.getClass.getSimpleName =>
          AggregatedRunQueryResponse.fromXmlString(breakdownTypes)(contents).flatMap{ rqr =>
            QepQueryDb.db.insertQueryResult(rqr.queryId, rqr.results.head)
            Log.debug(s"Inserted result from ${rqr.results.head.description} for query ${rqr.queryId}")
            QepQueryDbChangeNotifier.triggerDataChangeFor(rqr.queryId)
            Success(unit)
          }
        case Envelope(contentsType, contents, shrineVersion) if contentsType == IncrementalQueryResult.incrementalQueryResultsEnvelopeContentsType =>
          val changeDate = System.currentTimeMillis()
          IncrementalQueryResult.seqFromJson(contents).flatMap { iqrs: Seq[IncrementalQueryResult] =>
            val rows = iqrs.map(iqr => QueryResultRow(
              resultId = -1L,
              networkQueryId = iqr.networkQueryId,
              instanceId = -1L,
              adapterNode = iqr.adapterNodeName,
              resultType = None,
              size = 0L,
              startDate = None,
              endDate = None,
              status = QueryResult.StatusType.valueOf(iqr.statusTypeName).get,
              statusMessage = Some(iqr.statusMessage),
              changeDate = changeDate
            ))

            QepQueryDb.db.insertQueryResultRows(rows)
            Log.debug(s"Inserted incremental results $iqrs")
            rows.headOption.foreach(row => QepQueryDbChangeNotifier.triggerDataChangeFor(row.networkQueryId))
            Success(unit)
          }
        case e:Envelope => Failure(UnexpectedMessageContentsTypeException(e,queue))
        case _ => Failure(new IllegalArgumentException(s"Received something other than an envelope from this queue: $envelopeJson"))
        }.transform({ s =>
        message.complete()
        Success(unit)
      },{ x =>
        x match {
          case NonFatal(nfx) => QepReceiverCouldNotDecodeMessage(envelopeJson,queue,x)
          case throwable =>  throw throwable//blow something up
        }
        message.complete() //complete anyway. Can't be interpreted, so we don't want to see it again
        Failure(x)
      })
    }

    def createQueue(nodeName:String):Queue = {

      //Either come back with the right exception to try again, or a Queue
      def tryToCreateQueue():Try[Queue] =
        MessageQueueService.service.createQueueIfAbsent(nodeName)

      def keepGoing(attempt:Try[Queue]):Try[Boolean] = attempt.transform({queue => Success(false)}, {
        case okIsh: CouldNotCompleteMomTaskButOKToRetryException => Success(true)
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

object QepReceiverUncaughtExceptionHandler extends Thread.UncaughtExceptionHandler {
  override def uncaughtException(thread: Thread, throwable: Throwable): Unit = QepReceiverThreadEndedByThrowable(thread,throwable)
}

class QueueReceiverContextListener extends ServletContextListener {


  override def contextInitialized(servletContextEvent: ServletContextEvent): Unit = {
    QepReceiver.start()
  }

  override def contextDestroyed(servletContextEvent: ServletContextEvent): Unit = {
    QepReceiver.stop()
    QepQueryDbChangeNotifier.scheduler.shutdown()
  }
}

case class UnexpectedMessageContentsTypeException(envelope: Envelope, queue: Queue) extends Exception(s"Could not interpret message with contents type of ${envelope.contentsType} from queue ${queue.name} from shrine version ${envelope.shrineVersion}")

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

case class QepReceiverThreadEndedByThrowable(thread:Thread, x:Throwable) extends AbstractProblem(ProblemSources.Qep) {

  override val throwable = Some(x)

  override def summary: String = s"The Qep Receiver's thread stopped because of an uncaught exception."

  override def description: String =
    s"""The Qep Receiver's thread ${thread.getName} stopped because of an uncaught exception"""
}