package net.shrine.hornetqclient

import java.util.UUID
import java.util.concurrent.TimeoutException

import akka.actor.ActorSystem
import net.shrine.config.ConfigExtensions
import net.shrine.hornetqmom.LocalHornetQMom.SimpleMessage
import net.shrine.log.Loggable
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
  * A simple HornetQMomWebClient that uses HornetQMomWebApi to createQueue,
  * deleteQueue, sendMessage, receiveMessage, getQueues, and sendReceipt
  *
  * @author yifan
  * @since 8/10/17
  */
object HornetQMomWebClient extends MessageQueueService with Loggable {

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
  //    info(s"Starting ActorSystem: ${actorSystem.name} for HornetQMomWebClient at time: ${actorSystem.startTime}")
  //    actorSystem
  //  } catch {
  //    case NonFatal(x) => {
  //      debug(s"NonFatalException thrown while starting ActorSystem for HornetQMomWebClient: ${x.getMessage}")
  //      throw x
  //    }
  //    case x: ExceptionInInitializerError => {
  //      debug(s"ExceptionInInitializerError thrown while starting ActorSystem for HornetQMomWebClient: ${x.getMessage}")
  //      throw x
  //    }
  //  }
  //
  //  def startServiceActor(): ActorRef = try {
  //    // the service actor replies to incoming HttpRequests
  //    val actor: ActorRef = system.actorOf(Props[HornetQMomWebClientServiceActor])
  //    info(s"Starting ServiceActor: ${actor.toString()} for HornetQMomWebClient")
  //    actor
  //  }
  //  catch {
  //    case NonFatal(x) => {
  //      debug(s"NonFatalException thrown while starting ServiceActor for HornetQMomWebClient: ${x.getMessage}")
  //      throw x
  //    }
  //    case x: ExceptionInInitializerError => {
  //      debug(s"ExceptionInInitializerError thrown while starting ServiceActor for HornetQMomWebClient: ${x.getMessage}")
  //      throw x
  //    }
  //  }

  val momUrl: String = webClientConfig.getString("serverUrl")

  override def createQueueIfAbsent(queueName: String): Try[Queue] = {
    val proposedQueue: Queue = Queue(queueName)
    val createQueueUrl = momUrl + s"/createQueue/${proposedQueue.name}"
    val request: HttpRequest = HttpRequest(HttpMethods.PUT, createQueueUrl)
    for {
      response: HttpResponse <- webApiTry(request,s"create queue $queueName")
      queue: Queue <- queueFromResponse(response, queueName)
    } yield queue
  }

  def webApiTry(request:HttpRequest,operation:String,timeLimit:Duration = webClientTimeOut):Try[HttpResponse] = {
    HttpClient.webApiTry(request,timeLimit).transform({ response =>
      if(response.status.isSuccess) Success(response)
      else Try {
        response.status match {
          case x if x == StatusCodes.RequestTimeout => throw CouldNotCompleteMomTaskButOKToRetryException(operation, Some(response.status), Some(response.entity.asString))
          case x if x == StatusCodes.NetworkConnectTimeout => throw CouldNotCompleteMomTaskButOKToRetryException(operation, Some(response.status), Some(response.entity.asString))
          case x if x == StatusCodes.NetworkReadTimeout => throw CouldNotCompleteMomTaskButOKToRetryException(operation, Some(response.status), Some(response.entity.asString))
          case _ => throw CouldNotCompleteMomTaskDoNotRetryException(operation, Some(response.status), Some(response.entity.asString))
        }
      }
    }, {
      case tx: TimeoutException => Failure(CouldNotCompleteMomTaskButOKToRetryException(operation, None, None, Some(tx)))
      case t => Failure(t)
    })
  }

  def queueFromResponse(response: HttpResponse, queueName: String): Try[Queue] = Try {
    if (response.status == StatusCodes.Created) {
      val queueString = response.entity.asString
      implicit val formats = Serialization.formats(NoTypeHints)
      read[Queue](queueString)(formats, manifest[Queue])
    } else {
      if ((response.status == StatusCodes.NotFound) ||
        (response.status == StatusCodes.RequestTimeout)) throw CouldNotCompleteMomTaskButOKToRetryException(s"create a queue named $queueName", Some(response.status), Some(response.entity.asString))
      else throw new CouldNotCompleteMomTaskDoNotRetryException(s"Response status is ${response.status}, not Created. Cannot make queue $queueName from this response: ${response.entity.asString}", Some(response.status), Some(response.entity.asString))
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
    val deleteQueueUrl = momUrl + s"/deleteQueue/${proposedQueue.name}"
    val request: HttpRequest = HttpRequest(HttpMethods.PUT, deleteQueueUrl)
    webApiTry(request, s"delete $queueName").map(r => unit) // todo StatusCodes.OK
  }

  override def queues: Try[Seq[Queue]] = {
    val getQueuesUrl = momUrl + s"/getQueues"
    val request: HttpRequest = HttpRequest(HttpMethods.GET, getQueuesUrl)
    for {
      response: HttpResponse <- webApiTry(request, "getQueues")
      allQueues: Seq[Queue] <- Try {
        val allQueues: String = response.entity.asString
        implicit val formats = Serialization.formats(NoTypeHints)
        read[Seq[Queue]](allQueues)(formats, manifest[Seq[Queue]])
      }
    } yield allQueues
  }

  override def send(contents: String, to: Queue): Try[Unit] = {

    debug(s"send to $to '$contents'")

    val sendMessageUrl = momUrl + s"/sendMessage/${to.name}"
    val request: HttpRequest = HttpRequest(
      method = HttpMethods.PUT,
      uri = sendMessageUrl,
      entity = HttpEntity(contents) //todo set contents as XML or json SHRINE-2215
    )
    for {
      response: HttpResponse <- webApiTry(request, s"send to ${to.name}")
    } yield response
  }

  //todo test receiving no message SHRINE-2213
  override def receive(from: Queue, timeout: Duration): Try[Option[Message]] = {
    val seconds = timeout.toSeconds
    val receiveMessageUrl = momUrl + s"/receiveMessage/${from.name}?timeOutSeconds=$seconds"
    val request: HttpRequest = HttpRequest(HttpMethods.GET, receiveMessageUrl)

    for {
    //use the time to make the API call plus the timeout for the long poll
      response: HttpResponse <- webApiTry(request, s"receive from ${from.name}", webClientTimeOut + timeout)
      messageResponse: Option[Message] <- messageOptionFromResponse(response, from)
    } yield messageResponse
  }

  def messageOptionFromResponse(response: HttpResponse, from: Queue): Try[Option[Message]] = Try {
    if (response.status == StatusCodes.NoContent) {
      None
    } else if (response.status == StatusCodes.OK) Some {
      val responseString = response.entity.asString
      SimpleMessage.fromJson(responseString)
    } else if ((response.status == StatusCodes.NotFound) || (response.status == StatusCodes.RequestTimeout) || (response.status == StatusCodes.InternalServerError)) {
      throw CouldNotCompleteMomTaskButOKToRetryException(s"receive a message from ${from.name}", Some(response.status), Some(response.entity.asString))
    } else {
      throw CouldNotCompleteMomTaskDoNotRetryException(s"make a Message from response ${response.entity.asString} for ${from.name}", Some(response.status), Some(response.entity.asString))
    }
  }.transform({ s =>
    val hornetQMessage = s.map(msg => HornetQClientMessage(UUID.fromString(msg.deliveryAttemptID), msg.contents))
    Success(hornetQMessage)
  }, { throwable =>
    throwable match {
      case NonFatal(x) => CouldNotInterpretHTTPResponseProblem(x, "create a Message", from.name, response.entity.asString)
      case _ => //Don't touch
    }
    Failure(throwable)
  })

  case class HornetQClientMessage private(messageID: UUID, messageContent: String) extends Message {

    override def contents: String = messageContent

    override def complete(): Try[Unit] = {
      val entity: HttpEntity = HttpEntity(messageID.toString)
      val completeMessageUrl: String = s"$momUrl/acknowledge"
      val request: HttpRequest = HttpRequest(HttpMethods.PUT, completeMessageUrl).withEntity(entity)
      for {
        response: HttpResponse <- webApiTry(request,s"complete message $messageID").transform({r =>
          info(s"Message ${this.messageID} completed with ${r.status}")
          Success(r)
        }, { throwable =>
          info(s"Message ${this.messageID} failed in its complete process due to ${throwable.getMessage}")
          Failure(throwable)
        })
      } yield response
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