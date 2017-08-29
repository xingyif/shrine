package net.shrine.metadata

import com.typesafe.config.Config
import net.shrine.config.ConfigExtensions
import net.shrine.log.Log
import net.shrine.messagequeueservice.{Message, MessageQueueService}
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

  val receiveMessageRunnable: Runnable = new Runnable {
    override def run(): Unit = {
      while(true) { //forever
        try {
          receiveAMessage()
        } catch {
          case NonFatal(x) => Log.error("Exception while receiving a message.",x)//todo new kind of problem
          //pass-through to blow up the thread, receive no more results, do something dramatic in UncaughtExceptionHandler.
        }
      }
    }
  }

  //create a daemon thread that long-polls for messages forever
  val pollingThread = new Thread(receiveMessageRunnable,"Receive message thread")
  pollingThread.setDaemon(true)
//todo   pollingThread.setUncaughtExceptionHandler()

  pollingThread.start()

  //todo maybe pass this in from outside and make the thing a case class
  val queue = MessageQueueService.service.createQueueIfAbsent(ConfigSource.config.getString("shrine.humanReadableNodeName"))
  val pollDuration = Duration("15 seconds") //todo from config

  lazy val config: Config = ConfigSource.config
  val shrineConfig = config.getConfig("shrine")

  val breakdownTypes: Set[ResultOutputType] = shrineConfig.getOptionConfigured("breakdownResultOutputTypes", ResultOutputTypes.fromConfig).getOrElse(Set.empty)

  def receiveAMessage(): Unit = {
    val message: Option[Message] = MessageQueueService.service.receive(queue, pollDuration) //todo make this configurable (and testable)
    message.foreach(interpretAMessage)
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