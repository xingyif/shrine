package net.shrine.hornetqclient

import akka.actor.{Actor, ActorRef, ActorRefFactory, ActorSystem, Props}
import net.shrine.log.Loggable
import net.shrine.messagequeueservice.{Message, MessageQueueService, MessageSerializer, Queue, QueueSerializer}
import net.shrine.source.ConfigSource
import org.json4s.native.Serialization
import org.json4s.native.Serialization.{read, write}
import org.json4s.{Formats, NoTypeHints}
import spray.http.{HttpEntity, HttpMethods, HttpRequest, HttpResponse, StatusCodes}

import scala.collection.immutable.Seq
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.{Duration, DurationLong, FiniteDuration}
import scala.util.{Failure, Success, Try}
import scala.util.control.NonFatal

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
      response: HttpResponse <- Try(HttpClient.webApiCall(request))
      queue: Queue <- queueFromResponse(response)
    } yield queue
  }

  def queueFromResponse(response: HttpResponse):Try[Queue] = Try {
    if(response.status == StatusCodes.Created) {
      val queueString = response.entity.asString
      implicit val formats = Serialization.formats(NoTypeHints) + new QueueSerializer
      read[Queue](queueString)(formats, manifest[Queue])
    } else {
      throw new IllegalStateException(s"Response status is ${response.status}, not Created. Cannot make a queue from this response: ${response.entity.asString}")
    }
  }.transform({ s =>
    Success(s)
  },{throwable =>
    throwable match {
      case NonFatal(x) => error(s"Unable to create a Queue from '${response.entity.asString}' due to exception",throwable)  //todo probably want to wrap more information into a new Throwable here
      case _ =>
    }
    Failure(throwable)

  })

  override def deleteQueue(queueName: String): Try[Unit] = {
    val proposedQueue: Queue = Queue(queueName)
    val deleteQueueUrl = momUrl + s"/deleteQueue/${proposedQueue.name}"
    val request: HttpRequest = HttpRequest(HttpMethods.PUT, deleteQueueUrl)
    Try(HttpClient.webApiCall(request)) // StatusCodes.OK
  }

  override def queues: Try[Seq[Queue]] = {
    val getQueuesUrl = momUrl + s"/getQueues"
    val request: HttpRequest = HttpRequest(HttpMethods.GET, getQueuesUrl)
    for {
      response: HttpResponse <- Try(HttpClient.webApiCall(request))
      allQueues: Seq[Queue] <- Try {
        val allQueues: String = response.entity.asString
        implicit val formats = Serialization.formats(NoTypeHints) + new QueueSerializer
        read[Seq[Queue]](allQueues)(formats, manifest[Seq[Queue]])
      }
    } yield allQueues
  }

  override def send(contents: String, to: Queue): Try[Unit] = {
    val sendMessageUrl = momUrl + s"/sendMessage/${to.name}"
    val request: HttpRequest = HttpRequest(
      method = HttpMethods.PUT,
      uri = sendMessageUrl,
      entity = HttpEntity(contents)  //todo set contents as XML or json
    )
    for {
      response: HttpResponse <- Try(HttpClient.webApiCall(request,3L seconds)) //todo configurable
    } yield response
  }

  //todo test receiving no message
  override def receive(from: Queue, timeout: Duration): Try[Option[Message]] = {
    val seconds = timeout.toSeconds
    val receiveMessageUrl = momUrl + s"/receiveMessage/${from.name}?timeOutSeconds=$seconds"
    val request: HttpRequest = HttpRequest(HttpMethods.GET, receiveMessageUrl)
    val httpRequestTimeout: FiniteDuration = (timeout.toSeconds + 1) second //todo configurable

    for {
      response: HttpResponse <- Try(HttpClient.webApiCall(request, httpRequestTimeout))
      messageResponse: Option[Message] <- messageOptionFromResponse(response)
    } yield (messageResponse)
  }

  def messageOptionFromResponse(response: HttpResponse):Try[Option[Message]] = Try {
    if(response.status == StatusCodes.NotFound) None
    else if (response.status == StatusCodes.OK) Some {
      val responseString = response.entity.asString
      val formats = Serialization.formats(NoTypeHints) + new MessageSerializer
      read[Message](responseString)(formats, manifest[Message])
    } else {
      throw new IllegalStateException(s"Response status is ${response.status}, not OK or NotFound. Cannot make a Message from this response: ${response.entity.asString}")
    }
  }.transform({ s =>
    Success(s)
  },{throwable =>
    throwable match {
      case NonFatal(x) => error(s"Unable to create a Message from '${response.entity.asString}' due to exception",throwable) //todo probably want to wrap more information into a new Throwable here
      case _ =>
    }
    Failure(throwable)
  })


  override def completeMessage(message: Message): Try[Unit] = {
    implicit val formats: Formats = Serialization.formats(NoTypeHints) + new MessageSerializer
    val messageString: String = write[Message](message)(formats)

    val entity: HttpEntity = HttpEntity(messageString)
    val completeMessageUrl: String = momUrl + s"/acknowledge" // HttpEntity
    val request: HttpRequest = HttpRequest(HttpMethods.PUT, completeMessageUrl).withEntity(entity)
    for {
      response: HttpResponse <- Try(HttpClient.webApiCall(request))
    } yield response
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