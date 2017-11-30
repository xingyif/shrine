package net.shrine.messagequeueclient

import java.util.concurrent.TimeoutException

import akka.actor.ActorSystem
import net.shrine.config.ConfigExtensions
import net.shrine.log.Loggable
import net.shrine.messagequeuemiddleware.LocalMessageQueueMiddleware.SimpleMessage
import net.shrine.messagequeueservice.{CouldNotCompleteMomTaskButOKToRetryException, CouldNotCompleteMomTaskDoNotRetryException, Message, MessageQueueService, Queue}
import net.shrine.problem.{AbstractProblem, ProblemSources}
import net.shrine.source.ConfigSource
import org.json4s.NoTypeHints
import org.json4s.native.Serialization
import org.json4s.native.Serialization.read
import spray.http.{HttpEntity, HttpMethods, HttpRequest, HttpResponse, StatusCodes}

import scala.collection.immutable.Seq
import scala.concurrent.duration.Duration
import scala.language.postfixOps
import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}

/**
  * A simple MessageQueueWebClient that uses MessageQueueWebApi to createQueue,
  * deleteQueue, sendMessage, receiveMessage, getQueues, and sendReceipt
  *
  * @author yifan
  * @since 8/10/17
  */
object MessageQueueWebClient extends MessageQueueService with Loggable {

  // we need an ActorSystem to host our application in
  implicit val system: ActorSystem = ActorSystem("momServer", ConfigSource.config)

  val configPath = "shrine.messagequeue.blockingq"

  def webClientConfig = ConfigSource.config.getConfig("shrine.messagequeue.blockingq")

  val webClientTimeOut: Duration = webClientConfig.get("webClientTimeOut", Duration(_))
  // TODO in SHRINE-2167: Extract and share a SHRINE actor system
  // the service actor replies to incoming HttpRequests
  //  implicit val serviceActor: ActorRef = startServiceActor()

  //  def startActorSystem(): ActorSystem = try {
  //    val actorSystem: ActorSystem = ActorSystem("momServer", ConfigSource.config)
  //    info(s"Starting ActorSystem: ${actorSystem.name} for MessageQueueWebClient at time: ${actorSystem.startTime}")
  //    actorSystem
  //  } catch {
  //    case NonFatal(x) => {
  //      debug(s"NonFatalException thrown while starting ActorSystem for MessageQueueWebClient: ${x.getMessage}")
  //      throw x
  //    }
  //    case x: ExceptionInInitializerError => {
  //      debug(s"ExceptionInInitializerError thrown while starting ActorSystem for MessageQueueWebClient: ${x.getMessage}")
  //      throw x
  //    }
  //  }
  //
  //  def startServiceActor(): ActorRef = try {
  //    // the service actor replies to incoming HttpRequests
  //    val actor: ActorRef = system.actorOf(Props[HornetQMomWebClientServiceActor])
  //    info(s"Starting ServiceActor: ${actor.toString()} for MessageQueueWebClient")
  //    actor
  //  }
  //  catch {
  //    case NonFatal(x) => {
  //      debug(s"NonFatalException thrown while starting ServiceActor for MessageQueueWebClient: ${x.getMessage}")
  //      throw x
  //    }
  //    case x: ExceptionInInitializerError => {
  //      debug(s"ExceptionInInitializerError thrown while starting ServiceActor for MessageQueueWebClient: ${x.getMessage}")
  //      throw x
  //    }
  //  }

  val momUrl: String = webClientConfig.getString("serverUrl")

  def webApiTry(request:HttpRequest,operation:String,timeLimit:Duration = webClientTimeOut):Try[HttpResponse] = {
    HttpClient.webApiTry(request,timeLimit).transform({ response =>
      if(response.status.isSuccess) Success(response)
      else Try {
        response.status match {
          case x if x == StatusCodes.RequestTimeout => throw CouldNotCompleteMomTaskButOKToRetryException(operation, Some(response.status), Some(response.entity.asString))
          case x if x == StatusCodes.NetworkConnectTimeout => throw CouldNotCompleteMomTaskButOKToRetryException(operation, Some(response.status), Some(response.entity.asString))
          case x if x == StatusCodes.NetworkReadTimeout => throw CouldNotCompleteMomTaskButOKToRetryException(operation, Some(response.status), Some(response.entity.asString))
          case x if x == StatusCodes.NotFound => throw CouldNotCompleteMomTaskDoNotRetryException(operation, Some(response.status), Some(response.entity.asString))
          case _ => {
            throw CouldNotCompleteMomTaskDoNotRetryException(operation, Some(response.status), Some(response.entity.asString))
          }
        }
      }
    }, {
      case tx: TimeoutException => Failure(CouldNotCompleteMomTaskButOKToRetryException(operation, None, None, Some(tx)))
      case t => Failure(t)
    })
  }

  override def createQueueIfAbsent(queueName: String): Try[Queue] = {
    val proposedQueue: Queue = Queue(queueName)
    val createQueueUrl = s"$momUrl/createQueue/${proposedQueue.name}"
    val request: HttpRequest = HttpRequest(HttpMethods.PUT, createQueueUrl)
    webApiTry(request,s"create queue $queueName").transform({ response =>
      queueFromResponse(response, queueName)
    }, { throwable =>
      error(s"Failed to create queue $queueName due to $throwable")
      Failure(throwable)
    })
  }

  def queueFromResponse(response: HttpResponse, queueName: String): Try[Queue] = Try {
    if (response.status == StatusCodes.Created) {
      val queueString = response.entity.asString
      implicit val formats = Serialization.formats(NoTypeHints)
      read[Queue](queueString)(formats, manifest[Queue])
    } else {
      throw CouldNotCompleteMomTaskDoNotRetryException(s"Response status is ${response.status}, not Created. Cannot make queue $queueName from this response: ${response.entity.asString}", Some(response.status), Some(response.entity.asString))
    }
  }.transform({ s =>
    Success(s)
  }, { throwable =>
    throwable match {
      case NonFatal(x) => CouldNotInterpretHTTPResponseProblem(x, "create a Queue", queueName, response.entity.asString)
      case _ =>
    }
    Failure(throwable)
  })

  val unit = ()
  override def deleteQueue(queueName: String): Try[Unit] = {
    val proposedQueue: Queue = Queue(queueName)
    val deleteQueueUrl = s"$momUrl/deleteQueue/${proposedQueue.name}"
    val request: HttpRequest = HttpRequest(HttpMethods.PUT, deleteQueueUrl)
    webApiTry(request, s"delete $queueName").transform({ r =>
      if (r.status == StatusCodes.OK) {
        info(s"Successfully deleted queue $queueName")
        Success(r)
      } else {
        debug(
          s"""Try to delete queue, HTTPResponse is a success but it does not contain an expected StatusCode
             |Expected StatusCodes: StatusCodes.OK, Actual StatusCodes: ${r.status}
             |Response: $r
           """.stripMargin)
        // todo Failure(DoNotRetryException)?
        Success(r)
      }
    }, { t =>
      error(s"Failed to deleteQueue $queueName due to $t")
      // Not interpreting UnprocessableEntity, because the client is
      // trying to delete a non-existing queue, which indicates that we're already at the desired state
      Failure(t)
    })
  }

  override def queues: Try[Seq[Queue]] = {
    val getQueuesUrl = s"$momUrl/getQueues"
    val request: HttpRequest = HttpRequest(HttpMethods.GET, getQueuesUrl)
    webApiTry(request, "getQueues").transform({ response: HttpResponse =>
      if (response.status == StatusCodes.OK) {
        val allQueues: String = response.entity.asString
        implicit val formats = Serialization.formats(NoTypeHints)
        Success(read[Seq[Queue]](allQueues)(formats, manifest[Seq[Queue]]))
      } else {
        debug(
          s"""Try to get all queues, HTTPResponse is a success but it does not contain an expected StatusCode
             |Expected StatusCodes: StatusCodes.OK, Actual StatusCodes: ${response.status}
             |HTTP Response: $response
           """.stripMargin)
        Failure(CouldNotCompleteMomTaskDoNotRetryException("get all queues", Some(response.status), Some(response.entity.asString)))
      }
    }, { t =>
      error(s"Failed to get all queues due to $t")
      Failure(t)
    })
  }

  override def send(contents: String, to: Queue): Try[Unit] = {
    debug(s"send to $to '$contents'")
    val sendMessageUrl = s"$momUrl/sendMessage/${to.name}"
    val request: HttpRequest = HttpRequest(
      method = HttpMethods.PUT,
      uri = sendMessageUrl,
      entity = HttpEntity(contents) //todo set contents as XML or json SHRINE-2215
    )
    webApiTry(request, s"send to ${to.name}").transform({s =>
      if (s.status == StatusCodes.Accepted) {
        info(s"Successfully sent Message $contents to Queue $to")
        Success(s)
      } else {
        debug(
          s"""Try to sendMessage $contents, HTTPResponse is a success but it does not contain an expected StatusCode
             |Expected StatusCodes: StatusCodes.Accepted, Actual StatusCodes: ${s.status}
             |Response: $s""".stripMargin)
        Failure(CouldNotCompleteMomTaskDoNotRetryException(s"send a Message to ${to.name}", Some(s.status), Some(s.entity.asString)))
      }
    }, { throwable =>
      throwable match {
        case CouldNotCompleteMomTaskDoNotRetryException(task, status, content, cause) => {
          if (status.get == StatusCodes.UnprocessableEntity) {
            Failure(CouldNotCompleteMomTaskButOKToRetryException(s"send a Message to ${to.name}", status, content, cause))
          }
        }
        case _ => //Don't touch
      }
      error(s"Failed to send Message $contents to Queue $to due to Error: $throwable", throwable)
      Failure(throwable)
    })
  }

  //todo test receiving no message SHRINE-2213
  override def receive(from: Queue, timeout: Duration): Try[Option[Message]] = {
    val seconds = timeout.toSeconds
    val receiveMessageUrl = s"$momUrl/receiveMessage/${from.name}?timeOutSeconds=$seconds"
    val request: HttpRequest = HttpRequest(HttpMethods.GET, receiveMessageUrl)

    //use the time to make the API call plus the timeout for the long poll
      webApiTry(request, s"receive from ${from.name}", webClientTimeOut + timeout).transform({ response =>
          messageOptionFromResponse(response, from)
      }, { throwable: Throwable =>
        throwable match {
          case CouldNotCompleteMomTaskDoNotRetryException(task, status, contents, cause) => {
            if (status.get == StatusCodes.UnprocessableEntity) {
              Failure(CouldNotCompleteMomTaskButOKToRetryException(s"make a Message from response $cause for ${from.name}", status, contents, cause))
            }
          }
          case _ => //Don't touch
        }
        Failure(throwable)
      })
  }

  def messageOptionFromResponse(response: HttpResponse, from: Queue): Try[Option[Message]] = Try {
    if (response.status == StatusCodes.NoContent) {
      info(s"No message received from Queue $from, HTTP Response $response")
      None
    } else if (response.status == StatusCodes.OK) Some {
      info(s"Non-empty Message received from Queue $from")
      val responseString = response.entity.asString
      SimpleMessage.fromJson(responseString)
    } else if (response.status == StatusCodes.UnprocessableEntity) Some {
      throw CouldNotCompleteMomTaskButOKToRetryException(s"make a Message from response ${response.entity.asString} for ${from.name}", Some(response.status), Some(response.entity.asString))
    } else {
      throw CouldNotCompleteMomTaskDoNotRetryException(s"make a Message from response ${response.entity.asString} for ${from.name}", Some(response.status), Some(response.entity.asString))
    }
  }.transform({ s =>
    val messageQueueClientMessageOpt: Option[MessageQueueClientMessage] = s.map(msg => MessageQueueClientMessage(msg.deliveryAttemptID, msg.contents))
    Success(messageQueueClientMessageOpt)
  }, { throwable =>
    throwable match {
      case NonFatal(x) => CouldNotInterpretHTTPResponseProblem(x, "create a Message", from.name, response.entity.asString)
      case _ => //Don't touch
    }
    Failure(throwable)
  })

  case class MessageQueueClientMessage private(messageID: String, messageContent: String) extends Message {

    override def contents: String = messageContent

    override def complete(): Try[Unit] = {
      val completeMessageUrl: String = s"$momUrl/acknowledge"
      val request: HttpRequest = HttpRequest(
        method = HttpMethods.PUT,
        uri = completeMessageUrl,
        entity = HttpEntity(this.messageID)
      )
      webApiTry(request,s"complete message $messageID").transform({r =>
        if (r.status == StatusCodes.OK) {
          info(s"Message ${this.messageID} completed with ${r.status}")
          Success(r)
        } else {
          debug(
            s"""Try to completeMessage $messageContent, HTTPResponse is a success but it does not contain an expected StatusCode
               | Expected StatusCodes: StatusCodes.OK, Actual StatusCodes: ${r.status}
               | Response: $r""".stripMargin)
          // todo Failure(DoNotRetryException)?
          Success(r)
        }
      }, { throwable =>
        throwable match {
          case CouldNotCompleteMomTaskDoNotRetryException(task, status, contents, cause) => {
            if (status.get == StatusCodes.UnprocessableEntity) {
              info(s"Try to completeMessage $messageContent, but message no longer exists, $contents")
              return Success(unit)
            }
          }
          case _ => //Don't touch
        }
        info(s"Message ${this.messageID} failed in its complete process due to ${throwable.getMessage}")
        Failure(throwable)
      })
    }
  }
}

case class CouldNotInterpretHTTPResponseProblem(x: Throwable, task: String, queueName: String, httpResponseString: String) extends AbstractProblem(ProblemSources.Hub) {

  override val throwable = Some(x)
  override val summary: String = s"Unable to $task due to exception"
  override val description: String = s"Unable to $task from queue $queueName due to exception $x, http response: $httpResponseString"
}

// TODO in SHRINE-2167: Extract and share a SHRINE actor system
//class HornetQMomWebClientServiceActor extends Actor with MetaDataService {
//
//  // the HttpService trait defines only one abstract member, which
//  // connects the services environment to the enclosing actor or test
//  def actorRefFactory: ActorRefFactory = context
//
//  // this actor only runs our route, but you could add
//  // other things here, like request stream processing
//  // or timeout handling
//  def receive: Receive = runRoute(route)
//
//  override implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
//
//  override def system: ActorSystem = context.system
//}