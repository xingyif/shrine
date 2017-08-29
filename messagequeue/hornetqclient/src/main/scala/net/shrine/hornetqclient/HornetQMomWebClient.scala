package net.shrine.hornetqclient

import akka.actor.{Actor, ActorRef, ActorRefFactory, ActorSystem, Props}
import net.shrine.log.Loggable
import net.shrine.messagequeueservice.{Message, MessageQueueService, MessageSerializer, Queue}
import net.shrine.source.ConfigSource
import org.json4s.native.Serialization
import org.json4s.native.Serialization.{read, write}
import org.json4s.{Formats, NoTypeHints}
import spray.http.{HttpEntity, HttpMethods, HttpRequest, HttpResponse}

import scala.collection.immutable.Seq
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration
import scala.util.Try
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
    val createQueueUrl = momUrl + s"/createQueue/$queueName"
    val request: HttpRequest = HttpRequest(HttpMethods.PUT, createQueueUrl)
    for {
      response: HttpResponse <- Try(HttpClient.webApiCall(request))
      queue: Queue <- Try {
        val queueString = response.entity.asString
        implicit val formats = Serialization.formats(NoTypeHints)
        read[Queue](queueString)(formats, manifest[Queue])
      }
    } yield queue
  }

  override def deleteQueue(queueName: String): Try[Unit] = {
    val deleteQueueUrl = momUrl + s"/deleteQueue/$queueName"
    val request: HttpRequest = HttpRequest(HttpMethods.PUT, deleteQueueUrl)
    val response: Try[Unit] = Try(HttpClient.webApiCall(request)) // StatusCodes.OK
    response
  }

  override def queues: Try[Seq[Queue]] = {
    val getQueuesUrl = momUrl + s"/getQueues"
    val request: HttpRequest = HttpRequest(HttpMethods.GET, getQueuesUrl)
    for {
      response: HttpResponse <- Try(HttpClient.webApiCall(request))
      allQueues: Seq[Queue] <- Try {
        val allQueues: String = response.entity.asString
        implicit val formats = Serialization.formats(NoTypeHints)
        read[Seq[Queue]](allQueues)(formats, manifest[Seq[Queue]])
      }
    } yield allQueues
  }

  override def send(contents: String, to: Queue): Try[Unit] = {
    val sendMessageUrl = momUrl + s"/sendMessage/$contents/${to.name}"
    val request: HttpRequest = HttpRequest(HttpMethods.PUT, sendMessageUrl)
    for {
      response: HttpResponse <- Try(HttpClient.webApiCall(request))
    } yield response
  }

  override def receive(from: Queue, timeout: Duration): Try[Option[Message]] = {
    val seconds = timeout.toSeconds
    val receiveMessageUrl = momUrl + s"/receiveMessage/${from.name}?timeOutSeconds=$seconds"
    val request: HttpRequest = HttpRequest(HttpMethods.GET, receiveMessageUrl)
    for {
      response: HttpResponse <- Try(HttpClient.webApiCall(request))
      responseString: String <- Try { response.entity.asString }
      formats <- Try { Serialization.formats(NoTypeHints) + new MessageSerializer}
      messageResponse: Message <- Try { read[Message](responseString)(formats, manifest[Message]) }
      messageResponse: Option[Message] <- Try { Option(messageResponse) }
    } yield messageResponse
  }

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