package net.shrine.hornetqclient

import java.util.UUID

import akka.actor.ActorSystem
import net.shrine.config.ConfigExtensions
import net.shrine.hornetqmom.MessageContainer
import net.shrine.log.Loggable
import net.shrine.messagequeueservice.{CouldNotCreateQueueButOKToRetryException, Message, MessageQueueService, Queue}
import net.shrine.source.ConfigSource
import org.json4s.NoTypeHints
import org.json4s.native.Serialization
import org.json4s.native.Serialization.read
import spray.http.{HttpEntity, HttpMethods, HttpRequest, HttpResponse, StatusCode, StatusCodes}

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

  val webClientTimeOutSecond: Duration = ConfigSource.config.get("shrine.messagequeue.hornetq.webClientTimeOutSecond", Duration(_))
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

  val momUrl: String = ConfigSource.config.getString("shrine.messagequeue.hornetq.serverUrl")

  override def createQueueIfAbsent(queueName: String): Try[Queue] = {
    val proposedQueue: Queue = Queue(queueName)
    val createQueueUrl = momUrl + s"/createQueue/${proposedQueue.name}"
    val request: HttpRequest = HttpRequest(HttpMethods.PUT, createQueueUrl)
    for {
      response: HttpResponse <- Try(HttpClient.webApiCall(request, webClientTimeOutSecond))
      queue: Queue <- queueFromResponse(response)
    } yield queue
  }

  def queueFromResponse(response: HttpResponse):Try[Queue] = Try {
    if(response.status == StatusCodes.Created) {
      val queueString = response.entity.asString
      implicit val formats = Serialization.formats(NoTypeHints)
      read[Queue](queueString)(formats, manifest[Queue])
    } else {
      if((response.status == StatusCodes.NotFound) ||
        (response.status == StatusCodes.RequestTimeout))throw new CouldNotCreateQueueButOKToRetryException(response.status,response.entity.asString)
      else throw new IllegalStateException(s"Response status is ${response.status}, not Created. Cannot make a queue from this response: ${response.entity.asString}") //todo more specific custom exception SHRINE-2213
    }
  }.transform({ s =>
    Success(s)
  },{throwable =>
    throwable match {
      case NonFatal(x) => error(s"Unable to create a Queue from '${response.entity.asString}' due to exception",throwable)  //todo probably want to wrap more information into a new Throwable here SHRINE-2213
      case _ =>
    }
    Failure(throwable)

  })

  override def deleteQueue(queueName: String): Try[Unit] = {
    val proposedQueue: Queue = Queue(queueName)
    val deleteQueueUrl = momUrl + s"/deleteQueue/${proposedQueue.name}"
    val request: HttpRequest = HttpRequest(HttpMethods.PUT, deleteQueueUrl)
    Try(HttpClient.webApiCall(request, webClientTimeOutSecond)) // StatusCodes.OK
  }

  override def queues: Try[Seq[Queue]] = {
    val getQueuesUrl = momUrl + s"/getQueues"
    val request: HttpRequest = HttpRequest(HttpMethods.GET, getQueuesUrl)
    for {
      response: HttpResponse <- Try(HttpClient.webApiCall(request, webClientTimeOutSecond))
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
      entity = HttpEntity(contents)  //todo set contents as XML or json SHRINE-2215
    )
    for {
      response: HttpResponse <- Try(HttpClient.webApiCall(request, webClientTimeOutSecond))
    } yield response
  }

  //todo test receiving no message SHRINE-2213
  override def receive(from: Queue, timeout: Duration): Try[Option[Message]] = {
    val seconds = timeout.toSeconds
    val receiveMessageUrl = momUrl + s"/receiveMessage/${from.name}?timeOutSeconds=$seconds"
    val request: HttpRequest = HttpRequest(HttpMethods.GET, receiveMessageUrl)

    for {
      response: HttpResponse <- Try(HttpClient.webApiCall(request, webClientTimeOutSecond))
      messageResponse: Option[Message] <- messageOptionFromResponse(response)
    } yield messageResponse
  }

  def messageOptionFromResponse(response: HttpResponse):Try[Option[Message]] = Try {
    if(response.status == StatusCodes.NotFound) None
    else if (response.status == StatusCodes.OK) Some {
      val responseString = response.entity.asString
      MessageContainer.fromJson(responseString)
    } else {
      throw new IllegalStateException(s"Response status is ${response.status}, not OK or NotFound. Cannot make a Message from this response: ${response.entity.asString}")
    }
  }.transform({ s =>
    val hornetQMessage = s.map(msg => HornetQClientMessage(UUID.fromString(msg.id), msg.contents))
    Success(hornetQMessage)
  },{throwable =>
    throwable match {
      case NonFatal(x) => error(s"Unable to create a Message from '${response.entity.asString}' due to exception",throwable) //todo probably want to report a Problem here SHRINE-2216
      case _ =>
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
        response: HttpResponse <- Try(HttpClient.webApiCall(request)).transform({r =>
          info(s"Message ${this.messageID} completed with ${r.status}")
          Success(r)
        }, { throwable =>
          debug(s"Message ${this.messageID} failed in its complete process due to ${throwable.getMessage}")
          Failure(throwable)
        })
      } yield response
    }
  }

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