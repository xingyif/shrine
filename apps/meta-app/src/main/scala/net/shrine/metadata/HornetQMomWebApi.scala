package net.shrine.metadata

import akka.event.Logging
import net.shrine.log.Loggable
import net.shrine.mom.{LocalHornetQMom, Message, MessageSerializer, Queue}
import org.json4s.native.JsonMethods._
import org.json4s.native.Serialization
import org.json4s.native.Serialization.write
import org.json4s.{JValue, NoTypeHints, _}
import spray.http.{HttpRequest, HttpResponse, StatusCodes}
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

  // SQS returns CreateQueueResult, which contains queueUrl: String
  lazy val createQueue: Route = path("createQueue" / Segment) { queueName =>
    put {
        val response: Queue = LocalHornetQMom.createQueueIfAbsent(queueName)
        implicit val formates = response.json4sMarshaller
        complete(StatusCodes.Created)
      }
  }


  // SQS takes in DeleteMessageRequest, which contains a queueUrl: String and a ReceiptHandle: String
  // returns a DeleteMessageResult, toString for debugging
  lazy val deleteQueue: Route = path("deleteQueue" / Segment) { queueName =>
   put {
      LocalHornetQMom.deleteQueue(queueName)
      complete(StatusCodes.OK)
    }
  }

  // SQS sendMessage(String queueUrl, String messageBody) => SendMessageResult
  lazy val sendMessage: Route = path("sendMessage" / Segment / Segment) { (messageContent, toQueue) =>
    put {
      LocalHornetQMom.send(messageContent, Queue.apply(toQueue))
      complete(StatusCodes.Accepted)
    }
  }

  // SQS ReceiveMessageResult receiveMessage(String queueUrl)
  lazy val receiveMessage: Route =
    get {
      path("receiveMessage" / Segment) { fromQueue =>
        parameter('timeOutDuration ? 20) { timeOutDuration =>
          val timeout: Duration = Duration.create(timeOutDuration, "seconds")
          val response: Message = LocalHornetQMom.receive(Queue.apply(fromQueue), timeout).get
          implicit val formats = Serialization.formats(NoTypeHints) + new MessageSerializer
          respondWithStatus(StatusCodes.OK) {
            complete(write[Message](response)(formats))
          }
        }
      }
    }


  // SQS has DeleteMessageResult deleteMessage(String queueUrl, String receiptHandle)
  lazy val acknowledge: Route = path("acknowledge") {
    entity(as[String]) { messageJSON =>
      put {
        implicit val formats: Formats = Serialization.formats(NoTypeHints) + new MessageSerializer
        val messageJValue: JValue = parse(messageJSON)
        try {
          val msg: Message = messageJValue.extract[Message](formats, manifest[Message])
          LocalHornetQMom.completeMessage(msg)
          complete(StatusCodes.NoContent)
        } catch {
          case x => {
              x.printStackTrace()
              throw x}
        }
      }
    }
  }

  // Returns the names of the queues created on this server. Seq[Any]
  lazy val getQueues: Route = path("getQueues") {
    get {
      val queues = LocalHornetQMom.queues
      println(s"queues in api: $queues")

      implicit val formats = Serialization.formats(NoTypeHints)
      val response = write(queues)
      respondWithStatus(StatusCodes.OK) {complete(response)}
    }
  }

}