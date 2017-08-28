package net.shrine.hornetqmom

import akka.event.Logging
import net.shrine.log.Loggable
import net.shrine.messagequeueservice.{Message, MessageSerializer, Queue}
import org.json4s.native.Serialization
import org.json4s.native.Serialization.{read, write}
import org.json4s.{Formats, NoTypeHints}
import spray.http.StatusCodes
import spray.routing.directives.LogEntry
import spray.routing.{HttpService, Route}

import scala.collection.immutable.Seq
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success, Try}
import scala.util.control.NonFatal
/**
  * A web API that provides access to the internal HornetQMom library.
  * Allows client to createQueue, deleteQueue, sendMessage, receiveMessage, getQueues, and sendReceipt
  *
  * Created by yifan on 7/24/17.
  */

trait HornetQMomWebApi extends HttpService
  with Loggable {

  def momRoute: Route = pathPrefix("mom") {
      put {
        createQueue ~
          sendMessage ~
          acknowledge
      } ~ receiveMessage ~ getQueues ~ deleteQueue
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
            respondWithStatus(StatusCodes.InternalServerError) {
              complete(s"HornetQ throws an exception while trying to create the queue $queueName," +
                s"HornetQ Server response: ${createdQueueTry.failed.get}")
            }
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
            respondWithStatus(StatusCodes.InternalServerError) {
              complete(s"HornetQ throws an exception while trying to delete the queue $queueName," +
                s"HornetQ Server response: ${deleteQueueTry.failed.get}")
            }
          }
        }
      }
    }
  }

  // SQS sendMessage(String queueUrl, String messageBody) => SendMessageResult
  def sendMessage: Route = path("sendMessage" / Segment / Segment) { (messageContent, toQueue) =>
    detach() {
      val sendTry: Try[Unit] = LocalHornetQMom.send(messageContent, Queue.apply(toQueue))
      sendTry match {
        case Success(v) => {
          complete(StatusCodes.Accepted)
        }
        case Failure(x) => {
          respondWithStatus(StatusCodes.InternalServerError) {
            complete(s"HornetQ throws an exception while trying to send a message to the queue $toQueue," +
              s"HornetQ Server response: ${sendTry.failed.get}")
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
            val receiveTry: Try[Option[Message]] = LocalHornetQMom.receive(Queue.apply(fromQueue), timeout)
            receiveTry match {
              case Success(optMessage) => {
                implicit val formats = Serialization.formats(NoTypeHints) + new MessageSerializer
                optMessage.fold(complete(StatusCodes.NotFound))(msg => complete(write(optMessage)(formats)))
              }
              case Failure(x) => {
                respondWithStatus(StatusCodes.InternalServerError) {
                  complete(s"HornetQ throws an exception while trying to send a message to the queue $fromQueue," +
                    s"HornetQ Server response: ${receiveTry.failed.get}")
                }
              }
            }
          }
        }
      }
    }

  // SQS has DeleteMessageResult deleteMessage(String queueUrl, String receiptHandle)
  def acknowledge: Route = path("acknowledge") {
    entity(as[String]) { messageJSON =>
      implicit val formats: Formats = Serialization.formats(NoTypeHints) + new MessageSerializer
      detach() {
        val msg: Message = read[Message](messageJSON)(formats, manifest[Message])
        val acknowledgeTry: Try[Unit] = LocalHornetQMom.completeMessage(msg)
        acknowledgeTry match {
          case Success(v) => {
            complete(StatusCodes.ResetContent)
          }
          case Failure(x) => {
            LogEntry(s"\n  Request: acknowledge/$messageJSON\n  Response: ${acknowledgeTry.get}", Logging.DebugLevel)
            respondWithStatus(StatusCodes.InternalServerError) {
              complete(s"HornetQ throws an exception while trying to complete the given message," +
                s"HornetQ Server response: ${acknowledgeTry.failed.get}")
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
              complete(write[Seq[Queue]](seqQueue)(formats))
            }
            case Failure(x) => {
              respondWithStatus(StatusCodes.InternalServerError) {
                complete(s"HornetQ throws an exception while trying to get all queue names," +
                  s"HornetQ Server response: ${getQueuesTry.failed.get}")
              }
            }
          }
        }
      }
    }
  }

}