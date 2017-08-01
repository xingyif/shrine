

package net.shrine.metadata

import akka.event.Logging
import net.shrine.log.Loggable
import net.shrine.mom.LocalHornetQMom.sessionFactory
import net.shrine.mom.{HornetQMom, LocalHornetQMom, Message, Queue}
import org.hornetq.api.core.client.ClientSession
import spray.http.{HttpRequest, HttpResponse, StatusCodes}
import spray.routing.directives.LogEntry
import spray.routing.{HttpService, Route}

import scala.collection.immutable.Seq
import scala.concurrent.duration.{Duration, _}
/**
  * Created by yifan on 7/24/17.
  */

// todo see SQS StatusCode
trait RemoteHornetQMom extends HornetQMom  // todo RemoteHornetQMom needs to be a trait to be mix in
  with HttpService
  with Loggable {

  lazy val routes: Route = logRequestResponse(logEntryForRequestResponse _) {
    //logging is controlled by Akka's config, slf4j, and log4j config
    createQueue ~
      deleteQueue ~
      sendMessage ~
      receiveMessage ~
      getQueues ~
      acknowledge
  }

  /** logs the request method, uri and response at info level */
  def logEntryForRequestResponse(req: HttpRequest): Any => Option[LogEntry] = {
    case res: HttpResponse => Some(LogEntry(s"\n  Request: $req\n  Response: $res", Logging.InfoLevel))
    case _ => None // other kind of responses
  }

  /** logs just the request method, uri and response status at info level */
  def logEntryForRequest(req: HttpRequest): Any => Option[LogEntry] = {
    case res: HttpResponse => Some(LogEntry(s"\n  Request: $req\n  Response status: ${res.status}", Logging.InfoLevel))
    case _ => None // other kind of responses
  }

  lazy val createQueue: Route = get {
    pathPrefix("createQueue") {
      parameter('queueName) { (queueName: String) =>

        val response: Queue = LocalHornetQMom.createQueueIfAbsent(queueName)
        implicit val formates = response.json4sMarshaller
          complete(StatusCodes.Created)
      }
    }
  }

  // SQS returns CreateQueueResult, which contains queueUrl: String
  override def createQueueIfAbsent(queueName: String): Queue = {
    LocalHornetQMom.createQueueIfAbsent(queueName)
  }

  lazy val deleteQueue: Route = pathPrefix("deleteQueue") {
    parameter('queueName) { (queueName: String) =>
      deleteQueue(queueName)
      complete(StatusCodes.OK)
    }
  }

  // SQS takes in DeleteMessageRequest, which contains a queueUrl: String and a ReceiptHandle: String
  // returns a DeleteMessageResult, toString for debugging
  override def deleteQueue(queueName: String): Unit = {
      LocalHornetQMom.deleteQueue(queueName)
  }

  lazy val sendMessage: Route = pathPrefix("sendMessage") {
    parameters('messageContent, 'toQueue) { (messageContent: String, toQueue: String) => {
      send(messageContent, Queue.apply(toQueue))
      complete(StatusCodes.Accepted)
    }
    }
  }

  // SQS sendMessage(String queueUrl, String messageBody) => SendMessageResult
  override def send(contents: String, to: Queue): Unit = {
    LocalHornetQMom.send(contents, to)
  }

  lazy val receiveMessage: Route = pathPrefix("receiveMessage") {
    parameters('fromQueue, 'timeOutDuration) { (fromQueue, timeOutDuration) => {
      val timeout: Duration = Duration.create(timeOutDuration)
      val response: Message = receive(Queue.apply(fromQueue), timeout).get
      implicit val formats = response.json4sMarshaller
      respondWithStatus(StatusCodes.OK){complete(response)}
      }
    }
  }

  // SQS ReceiveMessageResult receiveMessage(String queueUrl)
  override def receive(from: Queue, timeout: Duration=10 second): Option[Message] = {
      LocalHornetQMom.receive(from, timeout)
  }

  lazy val acknowledge = pathPrefix("acknowledge") {
    parameter('message) { (message: String) =>
      val session: ClientSession = sessionFactory.createSession()
      val message = session.createMessage(false)
      val currentMessage: Message = Message(message)
      completeMessage(currentMessage)
      complete(StatusCodes.NoContent)
    }
  }

  // SQS has DeleteMessageResult deleteMessage(String queueUrl, String receiptHandle)
  override def completeMessage(message: Message): Unit = {
//    Try(StatusCodes.OK -> LocalHornetQMom.completeMessage(message))
//      .getOrElse(StatusCodes.BadGateway ->
//        s"HornetQException occurred while acknowledging the message!")
    LocalHornetQMom.completeMessage(message)
  }


  // Returns the names of the queues created on this server. Seq[Any]
  lazy val getQueues: Route = pathPrefix("getQueues") {
    val response: Seq[Queue] = this.queues
//    implicit val formats = response.asInstanceOf[ToResponseMarshallable]
//    val json: Json = Json(response)
//    val format: String = Json.format(json)(humanReadable())
//    complete(format) // todo turn queues into JSON
    complete(StatusCodes.OK)
  }

  override def queues: Seq[Queue] = LocalHornetQMom.queues
}