package net.shrine.metadata

import akka.actor.{ActorRef, ActorSystem}
import akka.event.Logging
import akka.io.IO
import net.shrine.log.Loggable
import net.shrine.mom.{HttpClient, Message, MessageQueueService, MessageSerializer, Queue}
import spray.can.Http
import spray.http.{HttpEntity, HttpMethods, HttpRequest, HttpResponse, StatusCode, StatusCodes, Uri}
import akka.pattern.ask
import net.shrine.source.ConfigSource
import org.json4s.{Formats, NoTypeHints}
import org.json4s.native.Serialization
import org.json4s.native.Serialization.{read, write}
import spray.routing.directives.LogEntry

import scala.collection.immutable.Seq
import scala.concurrent.duration.DurationInt
import scala.collection.immutable
import scala.concurrent.Future
import scala.concurrent.duration.Duration

/**
  * A simple HornetQMomWebClient that uses HornetQMomWebApi to createQueue,
  * deleteQueue, sendMessage, receiveMessage, getQueues, and sendReceipt
  *
  * @author yifan
  * @since 8/10/17
  */
object HornetQMomWebClient extends MessageQueueService with Loggable {
  implicit val system = ActorSystem()
  //   implicit val system = ActorSystem("dashboardServer",ConfigSource.config)
  import system.dispatcher

  val momUrl: String = ConfigSource.config.getString("shrine.metaData.hornetQMomUrl")


  override def createQueueIfAbsent(queueName: String): Queue = {
    val createQueueUrl = momUrl + s"/createQueue/$queueName"
    val request: HttpRequest = HttpRequest(HttpMethods.PUT, createQueueUrl)
      val response: HttpResponse = HttpClient.webApiCall(request)
      if (response.status == StatusCodes.Created) {
      val queueString = response.entity.asString
      implicit val formats = Serialization.formats(NoTypeHints)
      val queue: Queue = read[Queue](queueString)(formats, manifest[Queue])
      queue
    } else {
        handleUnsuccessfulRequest(request, response)
      }
  }

  override def deleteQueue(queueName: String): Unit = {
    val deleteQueueUrl = momUrl + s"/deleteQueue/$queueName"
    val request: HttpRequest = HttpRequest(HttpMethods.PUT, deleteQueueUrl)
    val response: HttpResponse = HttpClient.webApiCall(request) // StatusCodes.OK
    if (response.status != StatusCodes.OK) {
      handleUnsuccessfulRequest(request, response)
    }
  }

  override def queues: Seq[Queue] = {
    val getQueuesUrl = momUrl + s"/getQueues"
    val request: HttpRequest = HttpRequest(HttpMethods.GET, getQueuesUrl)

    val response: HttpResponse = HttpClient.webApiCall(request)
    if (response.status == StatusCodes.OK) {
      val allQueues: String = response.entity.asString
      implicit val formats = Serialization.formats(NoTypeHints)
      val queues: Seq[Queue] = read[Seq[Queue]](allQueues)(formats, manifest[Seq[Queue]])
      queues
    } else {
      handleUnsuccessfulRequest(request, response)
    }
  }

  override def send(contents: String, to: Queue): Unit = {
    val sendMessageUrl = momUrl + s"/sendMessage/$contents/$to"
    val request: HttpRequest = HttpRequest(HttpMethods.PUT, sendMessageUrl)
    val response: HttpResponse = HttpClient.webApiCall(request)
    if (response.status != StatusCodes.Accepted) {
      handleUnsuccessfulRequest(request, response)
    }
  }

  override def receive(from: Queue, timeout: Duration): Option[Message] = {
    val seconds = timeout.toSeconds
    val receiveMessageUrl = momUrl + s"/receiveMessage/$from?timeOutSeconds=$seconds"
    val request: HttpRequest = HttpRequest(HttpMethods.GET, receiveMessageUrl)
    val response: HttpResponse = HttpClient.webApiCall(request)
    if (response.status == StatusCodes.OK) {
      val responseString: String = response.entity.asString
      implicit val formats = Serialization.formats(NoTypeHints) + new MessageSerializer
      val messageResponse: Message = read[Message](responseString)(formats, manifest[Message])
      val result: Option[Message] = Option(messageResponse)
      result
    } else {
      handleUnsuccessfulRequest(request, response)
    }
  }

  override def completeMessage(message: Message): Unit = {
    implicit val formats: Formats = Serialization.formats(NoTypeHints) + new MessageSerializer
    val messageString: String = write[Message](message)(formats)

    val entity: HttpEntity = HttpEntity(messageString)
    val completeMessageUrl: String = momUrl + s"/acknowledge/$entity" // HttpEntity
    val request: HttpRequest = HttpRequest(HttpMethods.PUT, completeMessageUrl)
    val response: HttpResponse = HttpClient.webApiCall(request)
    if (response.status != StatusCodes.NoContent) {
      handleUnsuccessfulRequest(request, response)
    }
  }

  private def handleUnsuccessfulRequest(request: HttpRequest, response: HttpResponse) = {
    LogEntry(s"\n  Request: ${request.uri} failed with response status: ${response.status}" +
      s"\n response entity: ${response.entity.asString}", Logging.DebugLevel)
    throw new Exception(s"\n  Request: ${request.uri} failed with response status: ${response.status}" +
      s"\n response entity: ${response.entity.asString}")
  }
}