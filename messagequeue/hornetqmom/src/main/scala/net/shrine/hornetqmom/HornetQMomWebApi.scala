package net.shrine.hornetqmom

import java.util.UUID

import net.shrine.log.Loggable
import net.shrine.messagequeueservice.{Message, Queue}
import net.shrine.problem.{AbstractProblem, ProblemSources}
import net.shrine.source.ConfigSource
import org.json4s.JsonAST.{JField, JObject}
import org.json4s.native.Serialization
import org.json4s.native.Serialization.{read, write}
import org.json4s.{CustomSerializer, Formats, JString, NoTypeHints}
import spray.http.StatusCodes
import spray.routing.{HttpService, Route}

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
            val receiveTry: Try[Option[Message]] = LocalHornetQMom.receive(Queue(fromQueue), timeout)
            receiveTry match {
              case Success(optMessage) => {
                optMessage.fold(complete(StatusCodes.NotFound)){msg =>
                  implicit val formats = Serialization.formats(NoTypeHints) + MessageSerializer
                  val messageJson = write(msg)
                  complete(messageJson)
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
      implicit val formats: Formats = Serialization.formats(NoTypeHints) + MessageSerializer
      detach() {
        val msg: Message = read[Message](messageUUID)(formats, manifest[Message])
        val id: UUID = msg.messageUUID
        val acknowledgeTry: Try[Unit] = LocalHornetQMom.completeMessage(id)
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

object MessageSerializer extends CustomSerializer[Message](format => (
  {
    case JObject(
    JField("hornetQClientMessage",
    JObject(
//    JField("type", JInt(msgType)) ::
//      JField("messageID", JInt(id)) ::
//      JField("durable", JBool(durable)) ::
//      JField("expiration", JInt(expiration)) ::
//      JField("timestamp", JInt(timestamp)) ::
//      JField("priority", JInt(priority)) ::
//      JField(Message.contentsKey, JString(contents)) ::
      JField("uuid", JString(messageUUID)) ::
        JField(Message.contentsKey, JString(contents))
        :: Nil))
      :: Nil) => {
//      val hornetQClientMessage: ClientMessageImpl = new ClientMessageImpl(msgType.toByte, durable, expiration.toLong, timestamp.toLong, priority.toByte, 0)
//      hornetQClientMessage.putStringProperty(Message.contentsKey, contents)
      val id: UUID = UUID.fromString(messageUUID)
      val message: Message = Message(id, contents)
      message
    }
  }, {
  case msg: Message =>
    JObject(
      JField("hornetQClientMessage",
      JObject(
        JField("uuid", JString(msg.messageUUID.toString)) ::
          JField(Message.contentsKey, JString(msg.contents))

          //        JField("type", JLong(msg.getClientMessage.getType)) ::
//          JField("messageID", JLong(msg.getClientMessage.getMessageID)) ::
//          JField("durable", JBool(msg.getClientMessage.isDurable)) ::
//          JField("expiration", JLong(msg.getClientMessage.getExpiration)) ::
//          JField("timestamp", JLong(msg.getClientMessage.getTimestamp)) ::
//          JField("priority", JLong(msg.getClientMessage.getPriority)) ::
//          JField(Message.contentsKey, JString(msg.contents)) ::
//          JField("belongsToQueue", JString(msg.getBelongedQueueName))
          :: Nil))
      :: Nil)
}
))

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
