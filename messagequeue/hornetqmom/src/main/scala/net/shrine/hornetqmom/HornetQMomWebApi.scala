package net.shrine.hornetqmom

import java.util.UUID

import net.shrine.log.Loggable
import net.shrine.messagequeueservice.{Message, Queue}
import net.shrine.problem.{AbstractProblem, ProblemSources}
import net.shrine.source.ConfigSource
import net.shrine.spray.DefaultJsonSupport
import org.hornetq.api.core.client.ClientMessage
import org.json4s.JsonAST.{JField, JObject}
import org.json4s.native.Serialization
import org.json4s.native.Serialization.{read, write}
import org.json4s.{CustomSerializer, Formats, JString, NoTypeHints, ShortTypeHints}
import spray.http.StatusCodes
import spray.routing.{HttpService, Route}

import scala.collection.concurrent.TrieMap
import scala.collection.immutable.Seq
import scala.concurrent.duration.Duration
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
  private val idToMessages: TrieMap[UUID, LocalHornetQMom.LocalHornetQMessage] = TrieMap.empty

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
            implicit val formats = Serialization.formats(NoTypeHints) + QueueSerializer
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
            val receiveTry: Try[Option[LocalHornetQMom.LocalHornetQMessage]] = LocalHornetQMom.receive(Queue(fromQueue), timeout)
            receiveTry match {
              case Success(optionMessage) => {
                optionMessage.fold(complete(StatusCodes.NotFound)){localHornetQMessage =>
                  // add message in the map with an unique UUID
                  val msgID = UUID.randomUUID()
                  idToMessages.getOrElseUpdate(msgID, localHornetQMessage)
                  complete(MessageContainer(msgID.toString, localHornetQMessage.getContents).toJson)
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
        Try {
          if (!idToMessages.contains(id)) {
            throw new NoSuchElementException(s"Cannot match given $id to any Message in HornetQ server! Message does not exist!")
          }
        }
        val acknowledgeTry: Try[Unit] = idToMessages(id).complete()
        acknowledgeTry match {
          case Success(v) => {
            complete(StatusCodes.ResetContent)
          }
          case Failure(x) => {
            internalServerErrorOccured(x, "acknowledge")
          }
        }
      }
    }
  }

  // Returns the names of the queues created on this server. Seq[Any]
  def getQueues: Route = path("getQueues") {
    get {
      detach() {
        implicit val formats = Serialization.formats(NoTypeHints) + QueueSerializer
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

object QueueSerializer extends CustomSerializer[Queue](format => (
  {
    case JObject(JField("name", JString(s)) :: Nil) => Queue(s)
  },
  {
    case queue: Queue =>
      JObject(JField("name", JString(queue.name)) :: Nil)
  }
))

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
