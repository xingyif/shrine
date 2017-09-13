package net.shrine.hornetqmom

import java.util.UUID

import net.shrine.log.{Log, Loggable}
import net.shrine.messagequeueservice.{Message, Queue}
import net.shrine.problem.{AbstractProblem, ProblemSources}
import net.shrine.source.ConfigSource
import org.json4s.native.Serialization
import org.json4s.native.Serialization.write
import org.json4s.{NoTypeHints, ShortTypeHints}
import spray.http.StatusCodes
import spray.routing.{HttpService, Route}

import scala.collection.concurrent.TrieMap
import scala.collection.immutable.Seq
import scala.concurrent.duration.Duration
import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}
/**
  * A web API that provides access to the internal HornetQMom library.
  * Allows client to createQueue, deleteQueue, sendMessage, receiveMessage, getQueues, and sendReceipt
  *
  * Created by yifan on 7/24/17.
  */

trait HornetQMomWebApi extends HttpService
  with Loggable {

  def enabled: Boolean = ConfigSource.config.getBoolean("shrine.messagequeue.hornetQWebApi.enabled")
  val warningMessage: String = "If you intend for this node to serve as this SHRINE network's messaging hub " +
                        "set shrine.messagequeue.hornetQWebApi.enabled to true in your shrine.conf." +
                        " You do not want to do this unless you are the hub admin!"

  // keep a map of messages and ids
  private val idToMessages: TrieMap[UUID, Message] = TrieMap.empty
  // a sentinel that monitors the hashmap of idToMessages, any message that has been outstanding for more than 3X or 10X
  // time-to-live need to get cleaned out of this map
  val messageTimeOutInMillis: Long = ConfigSource.config.getLong("shrine.messagequeue.hornetQWebApi.messageTimeOutInMillis")
  val sentinelThread: Thread = new Thread(MapSentinelRunner(messageTimeOutInMillis),s"${getClass.getSimpleName} mapWatcher")
  sentinelThread.setDaemon(true)

  sentinelThread.start()
  Log.debug(s"Started the sentinel thread that cleans up outstanding messages that exceed $messageTimeOutInMillis milliseconds")

  case class MapSentinelRunner(timeOutInMillis: Long) extends Runnable {
    // watches the map
    override def run(): Unit = {
      val currentTimeInMillis = System.currentTimeMillis()
      while (true) {
        //forever
        try {
          Log.debug("About to clean up outstanding messages.")
          idToMessages.retain({ (uuid, localHornetQMessage) =>
            (currentTimeInMillis - localHornetQMessage.createdTimeInMillis) < timeOutInMillis
          })
          Log.debug("Outstanding messages have been cleaned from the map.")
        } catch {
          case NonFatal(x) => ExceptionWhileCleaningUpMessageProblem(timeOutInMillis, x)
          //pass-through to blow up the thread, receive no more results, do something dramatic in UncaughtExceptionHandler.
          case x => Log.error("Fatal exception while cleaning up outstanding messages", x)
            throw x
        }
      }
    }
  }

  def momRoute: Route = pathPrefix("mom") {

    if (!enabled) {
      val configProblem: CannotUseHornetQMomWebApiProblem = CannotUseHornetQMomWebApiProblem(new UnsupportedOperationException)
      warn(s"HornetQMomWebApi is not available to use due to configProblem ${configProblem.description}!")
      respondWithStatus(StatusCodes.NotFound) {
        complete(warningMessage)
      }
    } else {
      put {
        createQueue ~
          sendMessage ~
          acknowledge
      } ~ receiveMessage ~ getQueues ~ deleteQueue
    }
  }

  // SQS returns CreateQueueResult, which contains queueUrl: String
  def createQueue: Route =
    path("createQueue" / Segment) { queueName =>
      detach() {
        val createdQueueTry: Try[Queue] = LocalHornetQMom.createQueueIfAbsent(queueName)
        createdQueueTry match {
          case Success(queue) => {
            implicit val formats = Serialization.formats(NoTypeHints)
            val response: String = write[Queue](queue)(formats)
            respondWithStatus(StatusCodes.Created) {
              complete(response)
            }
          }
          case Failure(x) => {
            internalServerErrorOccured(x, "createQueue")
          }
        }
      }
    }

  // SQS takes in DeleteMessageRequest, which contains a queueUrl: String and a ReceiptHandle: String
  // returns a DeleteMessageResult, toString for debugging
  def deleteQueue: Route = path("deleteQueue" / Segment) { queueName =>
    put {
      detach() {
        val deleteQueueTry: Try[Unit] = LocalHornetQMom.deleteQueue(queueName)
        deleteQueueTry match {
          case Success(v) => {
            complete(StatusCodes.OK)
          }
          case Failure(x) => {
            internalServerErrorOccured(x, "deleteQueue")
          }
        }
      }
    }
  }

  // SQS sendMessage(String queueUrl, String messageBody) => SendMessageResult
  def sendMessage: Route = path("sendMessage" / Segment) { toQueue =>
    requestInstance { request =>
      val messageContent = request.entity.asString

      debug(s"sendMessage to $toQueue '$messageContent'")

      detach() {
        val sendTry: Try[Unit] = LocalHornetQMom.send(messageContent, Queue(toQueue))
        sendTry match {
          case Success(v) => {
            complete(StatusCodes.Accepted)
          }
          case Failure(x) => {
            internalServerErrorOccured(x, "sendMessage")
          }
        }
      }
    }
  }

  // SQS ReceiveMessageResult receiveMessage(String queueUrl)
  def receiveMessage: Route =
    get {
      path("receiveMessage" / Segment) { fromQueue =>
        parameter('timeOutSeconds ? 20) { timeOutSeconds =>
          val timeout: Duration = Duration.create(timeOutSeconds, "seconds")
          detach() {
            val receiveTry: Try[Option[Message]] = LocalHornetQMom.receive(Queue(fromQueue), timeout)
            receiveTry match {
              case Success(optionMessage) => {
                optionMessage.fold(complete(StatusCodes.NotFound)){localHornetQMessage =>
                  // add message in the map with an unique UUID
                  val msgID = UUID.randomUUID()
                  idToMessages.getOrElseUpdate(msgID, localHornetQMessage)
                  complete(MessageContainer(msgID.toString, localHornetQMessage.contents).toJson)
                }
              }
              case Failure(x) => {
                internalServerErrorOccured(x, "receiveMessage")
              }
            }
          }
        }
      }
    }

  // SQS has DeleteMessageResult deleteMessage(String queueUrl, String receiptHandle)
  def acknowledge: Route = path("acknowledge") {
    entity(as[String]) { messageUUID =>
      detach() {
        val id: UUID = UUID.fromString(messageUUID)
        // retrieve the localMessage from the concurrent hashmap
        val getMessageTry: Try[Message] = Try {
          idToMessages(id)
        }.transform({ message =>
          Success(message)
        }, { throwable =>
          Failure(MessageDoesNotExistException(id))
        })

        getMessageTry match {
          case Success(message) => {
            message.complete()
            complete(StatusCodes.ResetContent)
          }
          case Failure(x) => {
            x match {
              case m: MessageDoesNotExistException => {
                respondWithStatus(StatusCodes.NotFound){ complete(m.getMessage) }
              }
              case _ => internalServerErrorOccured(x, "acknowledge")
            }
          }
        }
      }
    }
  }

  // Returns the names of the queues created on this server. Seq[Any]
  def getQueues: Route = path("getQueues") {
    get {
      detach() {
        implicit val formats = Serialization.formats(NoTypeHints)
        respondWithStatus(StatusCodes.OK) {
          val getQueuesTry: Try[Seq[Queue]] = LocalHornetQMom.queues
          getQueuesTry match {
            case Success(seqQueue) => {
              complete(write[Seq[Queue]](LocalHornetQMom.queues.get)(formats))
            }
            case Failure(x) => {
              internalServerErrorOccured(x, "getQueues")
            }
          }
        }
      }
    }
  }

  def internalServerErrorOccured(x: Throwable, function: String): Route = {
    respondWithStatus(StatusCodes.InternalServerError) {
      val serverErrorProblem: HornetQMomServerErrorProblem = HornetQMomServerErrorProblem(x, function)
      debug(s"HornetQ encountered a Problem during $function, Problem Details: $serverErrorProblem")
      complete(s"HornetQ throws an exception while trying to $function. HornetQ Server response: ${x.getMessage}" +
        s"Exception is from ${x.getClass}")
    }
  }

}


case class MessageContainer(id: String, contents: String) {
  def toJson: String = {
    Serialization.write(this)(MessageContainer.messageFormats)
  }
}

object MessageContainer {
  val messageFormats = Serialization.formats(ShortTypeHints(List(classOf[MessageContainer])))

  def fromJson(jsonString: String): MessageContainer = {
    implicit val formats = messageFormats
    Serialization.read[MessageContainer](jsonString)
  }
}


case class HornetQMomServerErrorProblem(x:Throwable, function:String) extends AbstractProblem(ProblemSources.Hub) {

  override val throwable = Some(x)
  override val summary: String = "SHRINE cannot use HornetQMomWebApi due to a server error occurred in hornetQ."
  override val description: String = s"HornetQ throws an exception while trying to $function," +
                                      s" the server's response is: ${x.getMessage} from ${x.getClass}."
}

case class CannotUseHornetQMomWebApiProblem(x:Throwable) extends AbstractProblem(ProblemSources.Hub) {

  override val throwable = Some(x)
  override val summary: String = "SHRINE cannot use HornetQMomWebApi due to configuration in shrine.conf."
  override val description: String = "If you intend for this node to serve as this SHRINE network's messaging hub " +
                              "set shrine.messagequeue.hornetQWebApi.enabled to true in your shrine.conf." +
                              " You do not want to do this unless you are the hub admin!"
}

case class MessageDoesNotExistException(id: UUID) extends Exception(s"Cannot match given ${id.toString} to any Message in HornetQ server! Message does not exist!")

case class ExceptionWhileCleaningUpMessageProblem(timeOutInMillis: Long, x:Throwable) extends AbstractProblem(ProblemSources.Hub) {

  override val throwable = Some(x)

  override def summary: String = s"The Hub encountered an exception while trying to " +
    s"cleanup messages that has been outstanding for more than $timeOutInMillis milliseconds"

  override def description: String = s"The Hub encountered an exception while trying to " +
    s"cleanup messages that has been received for more than $timeOutInMillis milliseconds " +
    s"on Thread ${Thread.currentThread().getName}: ${x.getMessage}"
}