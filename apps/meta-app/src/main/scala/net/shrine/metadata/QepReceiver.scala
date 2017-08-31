package net.shrine.metadata

import com.typesafe.config.Config
import net.shrine.config.ConfigExtensions
import net.shrine.log.Log
import net.shrine.messagequeueservice.{Message, MessageQueueService}
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
//todo in 1.24, look into a streaming API for messages
object QepReceiver {

  Log.debug("started init of QepReceiver")

  val receiveMessageRunnable: Runnable = new Runnable {
    override def run(): Unit = {
      while(true) { //forever
        try {
          Log.debug("About to call receive.")
          receiveAMessage()
          Log.debug("Successfully called receive.")
        } catch {
          case NonFatal(x) => Log.error("Exception while receiving a message.",x)//todo new kind of problem
          //pass-through to blow up the thread, receive no more results, do something dramatic in UncaughtExceptionHandler.
          case x => Log.error("Fatal exception while receiving a message",x)
                    throw x
        }
      }
    }
  }

  Log.debug("Made the runnable for QepReceiver")

  //create a daemon thread that long-polls for messages forever
  val pollingThread = new Thread(receiveMessageRunnable,"Receive message thread")
  pollingThread.setDaemon(true)
//todo   pollingThread.setUncaughtExceptionHandler()

  Log.debug("made the thread for QepReceiver")

  //todo maybe pass this in from outside and make the thing a case class
  lazy val queue = MessageQueueService.service.createQueueIfAbsent(ConfigSource.config.getString("shrine.humanReadableNodeName")).get //todo better than get. handle errors
  val pollDuration = Duration("15 seconds") //todo from config

  val config: Config = ConfigSource.config
  val shrineConfig = config.getConfig("shrine")

  val breakdownTypes: Set[ResultOutputType] = shrineConfig.getOptionConfigured("breakdownResultOutputTypes", ResultOutputTypes.fromConfig).getOrElse(Set.empty)

  pollingThread.start()

  Log.debug("Started the QepReceiver thread")

  def receiveAMessage(): Unit = {
    Log.debug(s"QepReceiver about to poll for a message on $queue")

    val maybeMessage: Try[Option[Message]] = MessageQueueService.service.receive(queue, pollDuration) //todo make this configurable (and testable)
    Log.debug(s"QepReceiver received $maybeMessage from $queue")

    maybeMessage.transform({m =>
      m.foreach(interpretAMessage)
      Success(m)
    },{x =>
      ProblemNotYetEncoded(s"Something went wrong during receive from $queue",x) //todo create full-up problem
      Failure(x)
    })
  }

  val unit = ()
  def interpretAMessage(message: Message):Unit = {
    Log.debug(s"Received a message from $queue")

    val xmlString = message.contents
    val rqrt: Try[RunQueryResponse] = RunQueryResponse.fromXmlString(breakdownTypes)(xmlString)
    rqrt.transform({ rqr =>
      QepQueryDb.db.insertQueryResult(rqr.queryId, rqr.singleNodeResult)
      message.complete()
      Success(unit)
    },{ x =>
      Failure(x)
    })
  }

  //HornetQMomWebClient.send(rqr.toXml(),queue)
}