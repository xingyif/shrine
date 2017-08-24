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
import scala.util.Try
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
        if (createdQueueTry.isFailure) {
          respondWithStatus(StatusCodes.InternalServerError) {
            complete(s"HornetQ throws an exception while trying to create the queue $queueName," +
              s"HornetQ Server response: ${createdQueueTry.get}")
          }
        } else {
          implicit val formats = Serialization.formats(NoTypeHints)
          val response: String = write[Queue](createdQueueTry.get)(formats)
          respondWithStatus(StatusCodes.Created) {
            complete(response)
          }
        }
      }
  }

  // SQS takes in DeleteMessageRequest, which contains a queueUrl: String and a ReceiptHandle: String
  // returns a DeleteMessageResult, toString for debugging
  def deleteQueue: Route = path("deleteQueue" / Segment) { queueName =>
    delete {
      detach() {
        val deleteQueueTry: Try[Unit] = LocalHornetQMom.deleteQueue(queueName)
        if (deleteQueueTry.isFailure) {
          respondWithStatus(StatusCodes.InternalServerError) {
            complete(s"HornetQ throws an exception while trying to delete the queue $queueName," +
              s"HornetQ Server response: ${deleteQueueTry.get}")
          }
        } else {
          complete(StatusCodes.OK)
        }
      }
    }
  }

  // SQS sendMessage(String queueUrl, String messageBody) => SendMessageResult
  def sendMessage: Route = path("sendMessage" / Segment / Segment) { (messageContent, toQueue) =>
    detach() {
      val sendTry: Try[Unit] = LocalHornetQMom.send(messageContent, Queue.apply(toQueue))
      if (sendTry.isFailure) {
        respondWithStatus(StatusCodes.InternalServerError) {
          complete(s"HornetQ throws an exception while trying to send a message to the queue $toQueue," +
            s"HornetQ Server response: ${sendTry.get}")
        }
      } else {
        complete(StatusCodes.Accepted)
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
            if (receiveTry.isFailure) {
              respondWithStatus(StatusCodes.InternalServerError) {
                complete(s"HornetQ throws an exception while trying to send a message to the queue $fromQueue," +
                  s"HornetQ Server response: ${receiveTry.get}")
              }
            } else {
              implicit val formats = Serialization.formats(NoTypeHints) + new MessageSerializer
              receiveTry.get.fold(complete(StatusCodes.NotFound))(msg => complete(write(receiveTry.get)(formats)))
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
        try {
          val msg: Message = read[Message](messageJSON)(formats, manifest[Message])
          val acknowledgeTry: Try[Unit] = LocalHornetQMom.completeMessage(msg)
          if (acknowledgeTry.isFailure) {
            LogEntry(s"\n  Request: acknowledge/$messageJSON\n  Response: ${acknowledgeTry.get}", Logging.DebugLevel)
            respondWithStatus(StatusCodes.InternalServerError) {
              complete(s"HornetQ throws an exception while trying to complete the given message," +
                s"HornetQ Server response: ${acknowledgeTry.get}")
            }
          } else {
            complete(StatusCodes.ResetContent)
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
          if (getQueuesTry.isFailure) {
            respondWithStatus(StatusCodes.InternalServerError) {
              complete(s"HornetQ throws an exception while trying to get all queue names," +
                s"HornetQ Server response: ${getQueuesTry.get}")
            }
          } else {
            complete(write[Seq[Queue]](LocalHornetQMom.queues.get)(formats))
          }
        }
      }
    }
  }

}