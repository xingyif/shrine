package net.shrine.metadata

import akka.event.Logging
import net.shrine.log.Loggable
import net.shrine.mom.{LocalHornetQMom, Message, MessageSerializer, Queue}
import org.json4s.native.JsonMethods._
import org.json4s.native.Serialization
import org.json4s.native.Serialization.write
import org.json4s.{JValue, NoTypeHints, _}
import spray.http.StatusCodes
import spray.routing.directives.LogEntry
import spray.routing.{HttpService, Route}

import scala.concurrent.duration.Duration
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
        deleteQueue ~
        sendMessage ~
        acknowledge
    } ~ receiveMessage ~ getQueues
  }

  // SQS returns CreateQueueResult, which contains queueUrl: String
  def createQueue: Route = path("createQueue" / Segment) { queueName =>
    val createdQueue: Queue = LocalHornetQMom.createQueueIfAbsent(queueName)
    implicit val formats = Serialization.formats(NoTypeHints)
    val response: String = write[Queue](createdQueue)(formats)
    respondWithStatus(StatusCodes.Created) { complete(response) }
  }


  // SQS takes in DeleteMessageRequest, which contains a queueUrl: String and a ReceiptHandle: String
  // returns a DeleteMessageResult, toString for debugging
  def deleteQueue: Route = path("deleteQueue" / Segment) { queueName =>
    LocalHornetQMom.deleteQueue(queueName)
    complete(StatusCodes.OK)
  }

  // SQS sendMessage(String queueUrl, String messageBody) => SendMessageResult
  def sendMessage: Route = path("sendMessage" / Segment / Segment) { (messageContent, toQueue) =>
    LocalHornetQMom.send(messageContent, Queue.apply(toQueue))
    complete(StatusCodes.Accepted)
  }

  // SQS ReceiveMessageResult receiveMessage(String queueUrl)
  def receiveMessage: Route =
    get {
      path("receiveMessage" / Segment) { fromQueue =>
        parameter('timeOutSeconds ? 20) { timeOutSeconds =>
          val timeout: Duration = Duration.create(timeOutSeconds, "seconds")
          val response: Message = LocalHornetQMom.receive(Queue.apply(fromQueue), timeout).get
          implicit val formats = Serialization.formats(NoTypeHints) + new MessageSerializer
          respondWithStatus(StatusCodes.OK) {
            complete(write[Message](response)(formats))
          }
        }
      }
    }

  // SQS has DeleteMessageResult deleteMessage(String queueUrl, String receiptHandle)
  def acknowledge: Route = path("acknowledge") {
    entity(as[String]) { messageJSON =>
      implicit val formats: Formats = Serialization.formats(NoTypeHints) + new MessageSerializer
      val messageJValue: JValue = parse(messageJSON)
      try {
        val msg: Message = messageJValue.extract[Message](formats, manifest[Message])
        LocalHornetQMom.completeMessage(msg)
        complete(StatusCodes.NoContent)
      } catch {
        case x => {
          LogEntry(s"\n  Request: acknowledge/$messageJSON\n  Response: $x", Logging.DebugLevel)
          throw x}
      }
    }
  }

  // Returns the names of the queues created on this server. Seq[Any]
  def getQueues: Route = path("getQueues") {
    get {
      val queues = LocalHornetQMom.queues

      implicit val formats = Serialization.formats(NoTypeHints)
      val response = write(queues)

      respondWithStatus(StatusCodes.OK) {complete(response)}
    }
  }

}