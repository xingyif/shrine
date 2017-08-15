package net.shrine.metadata

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import net.shrine.log.Loggable
import net.shrine.mom.{HttpClient, Message, MessageQueueService, MessageSerializer, Queue}
import net.shrine.source.ConfigSource
import org.json4s.native.Serialization
import org.json4s.native.Serialization.{read, write}
import org.json4s.{Formats, NoTypeHints}
import spray.http.{HttpEntity, HttpMethods, HttpRequest, HttpResponse, StatusCodes}

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
  implicit val system: ActorSystem = startActorSystem()

  // the service actor replies to incoming HttpRequests
  implicit val serviceActor: ActorRef = startServiceActor()

  def startActorSystem(): ActorSystem = try {
    val actorSystem: ActorSystem = ActorSystem("momServer",ConfigSource.config)
    info(s"Starting ActorSystem: ${actorSystem.name} for HornetQMomWebClient at time: ${actorSystem.startTime}")
    actorSystem
  } catch {
    case NonFatal(x) => {
      debug(s"NonFatalException thrown while starting ActorSystem for HornetQMomWebClient: ${x.getMessage}")
      throw x
    }
    case x: ExceptionInInitializerError => {
      debug(s"ExceptionInInitializerError thrown while starting ActorSystem for HornetQMomWebClient: ${x.getMessage}")
      throw x
    }
  }

  def startServiceActor(): ActorRef = try {
    // the service actor replies to incoming HttpRequests
    val actor: ActorRef = system.actorOf(Props[HornetQMomWebClientServiceActor])
    info(s"Starting ServiceActor: ${actor.toString()} for HornetQMomWebClient")
    actor
  }
  catch {
    case NonFatal(x) => {
      debug(s"NonFatalException thrown while starting ServiceActor for HornetQMomWebClient: ${x.getMessage}")
      throw x
    }
    case x: ExceptionInInitializerError => {
      debug(s"ExceptionInInitializerError thrown while starting ServiceActor for HornetQMomWebClient: ${x.getMessage}")
      throw x
    }
  }

  val momUrl: String = ConfigSource.config.getString("shrine.mom.hornetq.serverUrl")

  override def createQueueIfAbsent(queueName: String): Queue = {
    val createQueueUrl = momUrl + s"/createQueue/$queueName"
    val request: HttpRequest = HttpRequest(HttpMethods.PUT, createQueueUrl)
    lazy val response: HttpResponse = HttpClient.webApiCall(request)
    Try ({
      response        // StatusCodes.Created
      info(s"\n Request: ${request.uri} succeeded with status: ${response.status}")
      val queueString = response.entity.asString
      implicit val formats = Serialization.formats(NoTypeHints)
      val queue: Queue = read[Queue](queueString)(formats, manifest[Queue])
      queue
    }).getOrElse({
      throw ReplyHasUnexpectedStatusCode(request, response)
    })
  }

  override def deleteQueue(queueName: String): Unit = {
    val deleteQueueUrl = momUrl + s"/deleteQueue/$queueName"
    val request: HttpRequest = HttpRequest(HttpMethods.PUT, deleteQueueUrl)
    lazy val response: HttpResponse = HttpClient.webApiCall(request) // StatusCodes.OK
    Try ({
      response
      info(s"\n Request: ${request.uri} succeeded with status: ${response.status}")
    }).getOrElse({
      throw ReplyHasUnexpectedStatusCode(request, response)
    })
  }

  override def queues: Seq[Queue] = {
    val getQueuesUrl = momUrl + s"/getQueues"
    val request: HttpRequest = HttpRequest(HttpMethods.GET, getQueuesUrl)

    lazy val response: HttpResponse = HttpClient.webApiCall(request)
    Try ({  // StatusCodes.OK
      response
      info(s"\n Request: ${request.uri} succeeded with status: ${response.status}")
      val allQueues: String = response.entity.asString
      implicit val formats = Serialization.formats(NoTypeHints)
      read[Seq[Queue]](allQueues)(formats, manifest[Seq[Queue]])
    }).getOrElse({
      throw ReplyHasUnexpectedStatusCode(request, response)
    })
  }

  override def send(contents: String, to: Queue): Unit = {
    val sendMessageUrl = momUrl + s"/sendMessage/$contents/${to.name}"
    val request: HttpRequest = HttpRequest(HttpMethods.PUT, sendMessageUrl)
    lazy val response: HttpResponse = HttpClient.webApiCall(request)
    Try({
      response     // StatusCodes.Accepted
      info(s"\n Request: ${request.uri} succeeded with status: ${response.status}")
    }).getOrElse({
      throw ReplyHasUnexpectedStatusCode(request, response)
    })
  }

  override def receive(from: Queue, timeout: Duration): Option[Message] = {
    val seconds = timeout.toSeconds
    val receiveMessageUrl = momUrl + s"/receiveMessage/${from.name}?timeOutSeconds=$seconds"
    val request: HttpRequest = HttpRequest(HttpMethods.GET, receiveMessageUrl)
    lazy val response: HttpResponse = HttpClient.webApiCall(request)
    Try({
      response // StatusCodes.OK
      info(s"\n Request: ${request.uri} succeeded with status: ${response.status}")
      val responseString: String = response.entity.asString
      implicit val formats = Serialization.formats(NoTypeHints) + new MessageSerializer
      val messageResponse: Message = read[Message](responseString)(formats, manifest[Message])
      val result: Option[Message] = Option(messageResponse)
      result
    }).getOrElse({
      throw ReplyHasUnexpectedStatusCode(request, response)
    })
  }

  override def completeMessage(message: Message): Unit = {
    implicit val formats: Formats = Serialization.formats(NoTypeHints) + new MessageSerializer
    val messageString: String = write[Message](message)(formats)

    val entity: HttpEntity = HttpEntity(messageString)
    val completeMessageUrl: String = momUrl + s"/acknowledge" // HttpEntity
    val request: HttpRequest = HttpRequest(HttpMethods.PUT, completeMessageUrl).withEntity(entity)
    lazy val response: HttpResponse = HttpClient.webApiCall(request)
    Try({
      response    // StatusCodes.NoContent
      info(s"\n Request: ${request.uri} succeeded with status: ${response.status}")
    }).getOrElse({
      if (response.status == StatusCodes.InternalServerError) {
        throw CouldNotDecipherGivenJsonAsSpecifiedException(request)
      }
      throw ReplyHasUnexpectedStatusCode(request, response)
    })
  }
}

class HornetQMomWebClientServiceActor extends Actor with MetaDataService {

  // the HttpService trait defines only one abstract member, which
  // connects the services environment to the enclosing actor or test
  def actorRefFactory = context

  // this actor only runs our route, but you could add
  // other things here, like request stream processing
  // or timeout handling
  def receive = runRoute(route)

  override implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
}

case class ReplyHasUnexpectedStatusCode(request: HttpRequest, response: HttpResponse) extends Exception {
  override def getMessage: String = {
    s"\n Request: ${request.uri} failed with response status: ${response.status}" +
      s"\n Response entity: ${response.entity.asString}"
  }

  def getFailureCause: String = s"The given request: $request has an unexpected response statusCode: ${response.status}"
}

case class CouldNotDecipherGivenJsonAsSpecifiedException(request: HttpRequest) extends Exception {
  override def getMessage: String = {
    s"\n Failed to decipher the given request: ${request.uri} given entity: ${request.entity}"
  }

  def getFailureCause: String = s"The given JSON entity should be a serialized JSON String of Message."

}