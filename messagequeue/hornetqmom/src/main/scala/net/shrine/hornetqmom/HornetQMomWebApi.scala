package net.shrine.hornetqmom

import akka.event.Logging
import net.shrine.log.Loggable
import net.shrine.messagequeueservice.{Message, MessageSerializer, Queue, QueueSerializer}
import net.shrine.problem.{AbstractProblem, ProblemSources}
import org.json4s.native.Serialization
import org.json4s.native.Serialization.{read, write}
import org.json4s.{Formats, NoTypeHints}
import spray.http.StatusCodes
import spray.routing.directives.LogEntry
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

  // todo have something here that wraps around momRoute, complete 404 with a message if config is false
  

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
            implicit val formats = Serialization.formats(NoTypeHints) + new QueueSerializer
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
                implicit val formats = Serialization.formats(NoTypeHints) + new MessageSerializer
                optMessage.fold(complete(StatusCodes.NotFound))(msg => complete(write(optMessage)(formats)))
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
        implicit val formats = Serialization.formats(NoTypeHints) + new QueueSerializer
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
      complete(s"HornetQ throws an exception while trying to $function. HornetQ Server response: ${x.getMessage} from ${x.getClass}")
    }
  }

}



case class HornetQMomServerErrorProblem(x:Throwable, function:String) extends AbstractProblem(ProblemSources.Adapter) {

  override val throwable = Some(x)
  override val summary: String = "SHRINE cannot use HornetQMomWebApi due to a server error occurred in hornetQ."
  override val description: String = s"HornetQ throws an exception while trying to $function," +
                                      s" the server's response is: ${x.getMessage} from ${x.getClass}."
}
