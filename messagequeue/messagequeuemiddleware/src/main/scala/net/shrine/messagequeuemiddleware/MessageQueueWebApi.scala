package net.shrine.messagequeuemiddleware

import java.util.UUID

import net.shrine.messagequeuemiddleware.LocalMessageQueueMiddleware.SimpleMessage
import net.shrine.log.Loggable
import net.shrine.messagequeueservice.{Message, Queue}
import net.shrine.problem.{AbstractProblem, ProblemSources}
import net.shrine.source.ConfigSource
import org.json4s.NoTypeHints
import org.json4s.native.Serialization
import org.json4s.native.Serialization.write
import spray.http.StatusCodes
import spray.routing.{HttpService, Route}

import scala.collection.immutable.Seq
import scala.concurrent.duration.Duration
import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}
/**
  * A web API that provides access to the internal MessageQueue library.
  * Allows client to createQueue, deleteQueue, sendMessage, receiveMessage, getQueues, and sendReceipt
  *
  * Created by yifan on 7/24/17.
  */

trait MessageQueueWebApi extends HttpService
  with Loggable {

  val configPath = "shrine.messagequeue.blockingqWebApi"
  def webApiConfig = ConfigSource.config.getConfig(configPath)

  //if(!webClientConfig.getConfigOrEmpty("serverUrl").isEmpty) webClientConfig.getString("serverUrl")
  def enabled: Boolean = webApiConfig.getBoolean("enabled")

  val warningMessage: String = "If you intend for this node to serve as this SHRINE network's messaging hub " +
                        "set shrine.messagequeue.blockingqWebApi.enabled to true in your shrine.conf." +
                        " You do not want to do this unless you are the hub admin!"
  if(!enabled) {
    debug(s"MessageQueueWebApi is not enabled.")
  }

  def momRoute: Route = pathPrefix("mom") {

    if (!enabled) {
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
        debug(s"Start createqueue/$queueName")
        val createdQueueTry: Try[Queue] = LocalMessageQueueMiddleware.createQueueIfAbsent(queueName)
        debug(s"createqueueTry is $createdQueueTry")
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
        val deleteQueueTry: Try[Unit] = LocalMessageQueueMiddleware.deleteQueue(queueName)
        deleteQueueTry match {
          case Success(v) => {
            complete(StatusCodes.OK)
          }
          case Failure(x) => {
            x match {
              case q: QueueDoesNotExistException => {
                respondWithStatus(StatusCodes.UnprocessableEntity) {
                  complete(s"${q.getMessage}")
                }
              }
              case NonFatal(nf) => {
                complete(s"Unable to delete queue '$queueName' due to exception $nf")
              }
              case _ => {
                throw x
              }
            }
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
        val sendTry: Try[Unit] = LocalMessageQueueMiddleware.send(messageContent, Queue(toQueue))
        sendTry match {
          case Success(v) => {
            complete(StatusCodes.Accepted)
          }
          case Failure(x) => {
            x match {
              case q: QueueDoesNotExistException => {
                respondWithStatus(StatusCodes.UnprocessableEntity) {
                  complete(s"${q.getMessage}")
                }
              }
              case NonFatal(nf) => {
                complete(s"Unable to send a Message to '$toQueue' due to exception $nf")
              }
              case _ => {
                throw x
              }
            }
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
            val receiveTry: Try[Option[Message]] = LocalMessageQueueMiddleware.receive(Queue(fromQueue), timeout)
            receiveTry match {
              case Success(optionMessage) => {
                optionMessage.fold(
                  respondWithStatus(StatusCodes.NoContent){
                  complete(s"No current Message available in queue $fromQueue!")
                }){ localMessage =>
                  val simpleMessage: SimpleMessage = localMessage.asInstanceOf[SimpleMessage]
                  complete(simpleMessage.toJson)
                }
              }
              case Failure(x) => {
                x match {
                  case q: QueueDoesNotExistException => {
                    respondWithStatus(StatusCodes.UnprocessableEntity) {
                      complete(s"${q.getMessage}")
                    }
                  }
                  case NonFatal(nf) => {
                    complete(s"Unable to receive a Message from '$fromQueue' due to exception $nf")
                  }
                  case _ => {
                    throw x
                  }
                }
              }
            }
          }
        }
      }
    }

  // SQS has DeleteMessageResult deleteMessage(String queueUrl, String receiptHandle)
  def acknowledge: Route = path("acknowledge") {
    requestInstance { request =>
      val deliveryAttemptID = request.entity.asString
      val id: UUID = UUID.fromString(deliveryAttemptID)
      detach() {
        val completeTry: Try[Unit] = LocalMessageQueueMiddleware.completeMessage(id)
        completeTry match {
          case Success(s) => {
            complete(StatusCodes.OK) // ResetContent causes connection timeout
          }
          case Failure(x) => {
            x match {
              case m: MessageDoesNotExistAndCannotBeCompletedException => {
                respondWithStatus(StatusCodes.UnprocessableEntity) { // todo should completeMessage return a success if message is already gone
                  complete(m.getMessage)
                }
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
          val getQueuesTry: Try[Seq[Queue]] = LocalMessageQueueMiddleware.queues
          getQueuesTry match {
            case Success(seqQueue) => {
              complete(write[Seq[Queue]](LocalMessageQueueMiddleware.queues.get)(formats))
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
      val serverErrorProblem: MessageQueueWebApiServerErrorProblem = MessageQueueWebApiServerErrorProblem(x, function)
      debug(s"MessageQueueMiddleware encountered a Problem during $function, Problem Details: $serverErrorProblem")
      complete(
        s"""MessageQueueMiddleware throws an exception while trying to $function.
           |MessageQueueMiddleware Server response: ${x.getMessage} Exception is from ${x.getClass}""".stripMargin)
    }
  }

}

case class MessageQueueWebApiServerErrorProblem(x:Throwable, function:String) extends AbstractProblem(ProblemSources.Hub) {

  override val throwable = Some(x)
  override val summary: String = "SHRINE cannot use MessageQueueWebApi due to a server error occurred in messageQueueMiddleware."
  override val description: String =
    s"""MessageQueueMiddleware throws an exception while trying to $function,
       |the server's response is: ${x.getMessage} from ${x.getClass}.""".stripMargin
}