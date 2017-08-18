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
        val createdQueue: Queue = LocalHornetQMom.createQueueIfAbsent(queueName)
        implicit val formats = Serialization.formats(NoTypeHints)
        val response: String = write[Queue](createdQueue)(formats)
        respondWithStatus(StatusCodes.Created) {
          complete(response)
        }
      }
  }

  // SQS takes in DeleteMessageRequest, which contains a queueUrl: String and a ReceiptHandle: String
  // returns a DeleteMessageResult, toString for debugging
  def deleteQueue: Route = path("deleteQueue" / Segment) { queueName =>
    delete {
      detach() {
        LocalHornetQMom.deleteQueue(queueName)
        complete(StatusCodes.OK)
      }
    }
  }

  // SQS sendMessage(String queueUrl, String messageBody) => SendMessageResult
  def sendMessage: Route = path("sendMessage" / Segment / Segment) { (messageContent, toQueue) =>
    detach() {
      LocalHornetQMom.send(messageContent, Queue.apply(toQueue))
      complete(StatusCodes.Accepted)
    }
  }

  // SQS ReceiveMessageResult receiveMessage(String queueUrl)
  def receiveMessage: Route =
    get {
      path("receiveMessage" / Segment) { fromQueue =>
        parameter('timeOutSeconds ? 20) { timeOutSeconds =>
          val timeout: Duration = Duration.create(timeOutSeconds, "seconds")
          detach() {
            val response: Option[Message] = LocalHornetQMom.receive(Queue.apply(fromQueue), timeout)
            implicit val formats = Serialization.formats(NoTypeHints) + new MessageSerializer
            response.fold(complete(StatusCodes.NotFound))(msg => complete(write(response)(formats)))
          }
        }
      }
    }

  // SQS has DeleteMessageResult deleteMessage(String queueUrl, String receiptHandle)
  def acknowledge: Route = path("acknowledge") {
    entity(as[String]) { messageJSON =>
      implicit val formats: Formats = Serialization.formats(NoTypeHints) + new MessageSerializer
      detach() {
        try {
          val msg: Message = read[Message](messageJSON)(formats, manifest[Message])
          LocalHornetQMom.completeMessage(msg)
          complete(StatusCodes.ResetContent)
        } catch {
          case NonFatal(x) => {
            LogEntry(s"\n  Request: acknowledge/$messageJSON\n  Response: $x", Logging.DebugLevel)
            complete(StatusCodes.BadRequest)
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
          complete(write[Seq[Queue]](LocalHornetQMom.queues)(formats))
        }
      }
    }
  }

}