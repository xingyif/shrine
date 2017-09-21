package net.shrine.hornetqmom

import java.util.UUID

import net.shrine.hornetqmom.LocalHornetQMom.SimpleMessage
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
  * A web API that provides access to the internal HornetQMom library.
  * Allows client to createQueue, deleteQueue, sendMessage, receiveMessage, getQueues, and sendReceipt
  *
  * Created by yifan on 7/24/17.
  */

trait HornetQMomWebApi extends HttpService
  with Loggable {

  val configPath = "shrine.messagequeue.blockingqWebApi"
  def webApiConfig = ConfigSource.config.getConfig(configPath)

  //if(!webClientConfig.getConfigOrEmpty("serverUrl").isEmpty) webClientConfig.getString("serverUrl")
  def enabled: Boolean = webApiConfig.getBoolean("enabled")

  val warningMessage: String = "If you intend for this node to serve as this SHRINE network's messaging hub " +
                        "set shrine.messagequeue.blockingqWebApi.enabled to true in your shrine.conf." +
                        " You do not want to do this unless you are the hub admin!"
  if(!enabled) {
    debug(s"HornetQMomWebApi is not enabled.")
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
        val createdQueueTry: Try[Queue] = LocalHornetQMom.createQueueIfAbsent(queueName)
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
        val deleteQueueTry: Try[Unit] = LocalHornetQMom.deleteQueue(queueName)
        deleteQueueTry match {
          case Success(v) => {
            complete(StatusCodes.OK)
          }
          case Failure(x) => {
            x match {
              case q: QueueDoesNotExistException => {
                respondWithStatus(StatusCodes.NotFound) {
                  complete(s"${q.getMessage}")
                }
              }
              case NonFatal(nf) => {
                complete(s"Unable to delete queue '$queueName' due to exception $nf")
              }
              case _ => {
                internalServerErrorOccured(x, "deleteQueue")
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
        val sendTry: Try[Unit] = LocalHornetQMom.send(messageContent, Queue(toQueue))
        sendTry match {
          case Success(v) => {
            complete(StatusCodes.Accepted)
          }
          case Failure(x) => {
            x match {
              case q: QueueDoesNotExistException => {
                respondWithStatus(StatusCodes.NotFound) {
                  complete(s"${q.getMessage}")
                }
              }
              case NonFatal(nf) => {
                complete(s"Unable to send a Message to '$toQueue' due to exception $nf")
              }
              case _ => {
                internalServerErrorOccured(x, "sendMessage")
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
            val receiveTry: Try[Option[Message]] = LocalHornetQMom.receive(Queue(fromQueue), timeout)
            receiveTry match {
              case Success(optionMessage) => {
                optionMessage.fold(
                  respondWithStatus(StatusCodes.NoContent){
                  complete(s"No current Message available in queue $fromQueue!")
                }){ localMessage =>
                  complete(SimpleMessage(localMessage.deliveryAttemptUUID.toString, localMessage.contents).toJson)
                }
              }
              case Failure(x) => {
                x match {
                  case q: QueueDoesNotExistException => {
                    respondWithStatus(StatusCodes.NotFound) {
                      complete(s"${q.getMessage}")
                    }
                  }
                  case NonFatal(nf) => {
                    complete(s"Unable to receive a Message from '$fromQueue' due to exception $nf")
                  }
                  case _ => {
                    internalServerErrorOccured(x, "receiveMessage")
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
    entity(as[String]) { deliveryAttemptID =>
      detach() {
        val id: UUID = UUID.fromString(deliveryAttemptID)
        val completeTry: Try[Unit] = LocalHornetQMom.completeMessage(id)
        completeTry match {
          case Success(s) => {
            complete(StatusCodes.ResetContent)
          }
          case Failure(x) => {
            x match {
              case m: MessageDoesNotExistAndCannotBeCompletedException => {
                respondWithStatus(StatusCodes.NotFound) {
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

//case class MessageContainer(id: String, contents: String) {
//  def toJson: String = {
//    Serialization.write(this)(MessageContainer.messageFormats)
//  }
//}
//
//object MessageContainer {
//  val messageFormats = Serialization.formats(ShortTypeHints(List(classOf[MessageContainer])))
//
//  def fromJson(jsonString: String): MessageContainer = {
//    implicit val formats = messageFormats
//    Serialization.read[MessageContainer](jsonString)
//  }
//}


case class HornetQMomServerErrorProblem(x:Throwable, function:String) extends AbstractProblem(ProblemSources.Hub) {

  override val throwable = Some(x)
  override val summary: String = "SHRINE cannot use HornetQMomWebApi due to a server error occurred in hornetQ."
  override val description: String = s"HornetQ throws an exception while trying to $function," +
                                      s" the server's response is: ${x.getMessage} from ${x.getClass}."
}

//todo is this used anywhere?
case class CannotUseHornetQMomWebApiProblem(x:Throwable) extends AbstractProblem(ProblemSources.Hub) {

  override val throwable = Some(x)
  override val summary: String = "SHRINE cannot use HornetQMomWebApi due to configuration in shrine.conf."
  override val description: String = "If you intend for this node to serve as this SHRINE network's messaging hub " +
                              "set shrine.messagequeue.hornetQWebApi.enabled to true in your shrine.conf." +
                              " You do not want to do this unless you are the hub admin!"
}


case class MessageDoesNotExistInMapProblem(id: UUID) extends AbstractProblem(ProblemSources.Hub) {

  override def summary: String = s"The client expected message $id, but the server did not find it and could not complete() the message."

  override def description: String = s"The client expected message $id from , but the server did not find it and could not complete() the message." +
    s" Message either has never been received or already been completed!"
}
