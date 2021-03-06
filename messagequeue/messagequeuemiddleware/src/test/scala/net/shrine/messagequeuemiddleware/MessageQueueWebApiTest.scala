package net.shrine.messagequeuemiddleware
import java.util.UUID
import java.util.concurrent.TimeUnit

import akka.actor.{ActorRefFactory, ActorSystem}
import net.shrine.messagequeuemiddleware.LocalMessageQueueMiddleware.SimpleMessage
import net.shrine.messagequeueservice.Queue
import net.shrine.source.ConfigSource
import net.shrine.config.ConfigExtensions
import org.json4s.NoTypeHints
import org.json4s.native.Serialization
import org.json4s.native.Serialization.read
import org.junit.runner.RunWith
import org.scalatest.FlatSpec
import org.scalatest.junit.JUnitRunner
import spray.http.HttpEntity
import spray.http.StatusCodes._
import spray.testkit.ScalatestRouteTest
import scala.concurrent.duration._

import scala.collection.immutable.Seq
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration.{Duration, DurationInt}
/**
  * Test basic functions of MessageQueueWebApi
  * Created by yifan on 7/27/17.
  */


@RunWith(classOf[JUnitRunner])
class MessageQueueWebApiTest extends FlatSpec with ScalatestRouteTest with MessageQueueWebApi {
  override def actorRefFactory: ActorRefFactory = system

  private val queue: Queue = Queue("test QueueInWebApi")
  private val queueName: String = queue.name // "testQueueInWebApi"
  private val messageContent = "test Content"
  implicit def default(implicit system: ActorSystem) = RouteTestTimeout(5.seconds)

  "MessageQueueWebApi" should "create/delete the given queue, send/receive message, get queues" in {
    val configMap: Map[String, String] = Map( "shrine.messagequeue.blockingqWebApi.enabled" -> "true",
      "shrine.messagequeue.blockingq.messageTimeToLive" -> "4 days",
      "shrine.messagequeue.blockingq.messageRedeliveryDelay" -> "3 seconds",
      "shrine.messagequeue.blockingq.messageMaxDeliveryAttempts" -> "2")

    ConfigSource.atomicConfig.configForBlock(configMap, "MessageQueueWebApiTest") {

      val messageRedeliveryDelay = ConfigSource.config.get("shrine.messagequeue.blockingq.messageRedeliveryDelay", Duration(_)).toMillis

      Get(s"/mom/getQueues") ~> momRoute ~> check {
        val response: String = new String(body.data.toByteArray)
        implicit val formats = Serialization.formats(NoTypeHints)
        val jsonToSeq: Seq[Queue] = read[Seq[Queue]](response)(formats, manifest[Seq[Queue]])

        assertResult(OK)(status)
        assert(jsonToSeq.isEmpty)
      }

      Put(s"/mom/createQueue/$queueName") ~> momRoute ~> check {
        val response = new String(body.data.toByteArray)
        implicit val formats = Serialization.formats(NoTypeHints)
        val jsonToQueue = read[Queue](response)(formats, manifest[Queue])
        val responseQueueName = jsonToQueue.name
        assertResult(Created)(status)
        assertResult(queueName)(responseQueueName)
      }

      // should be OK to create a queue twice
      Put(s"/mom/createQueue/$queueName") ~> momRoute ~> check {
        val response = new String(body.data.toByteArray)
        implicit val formats = Serialization.formats(NoTypeHints)
        val jsonToQueue = read[Queue](response)(formats, manifest[Queue])
        val responseQueueName = jsonToQueue.name
        assertResult(Created)(status)
        assertResult(queueName)(responseQueueName)
      }

      Put(s"/mom/sendMessage/$queueName", HttpEntity(s"$messageContent")) ~> momRoute ~> check {
        assertResult(Accepted)(status)
      }

      Get(s"/mom/getQueues") ~> momRoute ~> check {
        val response: String = new String(body.data.toByteArray)
        implicit val formats = Serialization.formats(NoTypeHints)
        val jsonToSeq: Seq[Queue] = read[Seq[Queue]](response)(formats, manifest[Seq[Queue]])

        assertResult(OK)(status)
        assertResult(queueName)(jsonToSeq.head.name)
      }

      val messageUUIDList: ArrayBuffer[String] = ArrayBuffer[String]()
      // given timeout is 2 seconds
      Get(s"/mom/receiveMessage/$queueName?timeOutSeconds=2") ~> momRoute ~> check {
        val response = new String(body.data.toByteArray)
        assertResult(OK)(status)
        val responseMsg: SimpleMessage = SimpleMessage.fromJson(response)
        messageUUIDList += responseMsg.deliveryAttemptID
        assertResult(responseMsg.contents)(messageContent)
      }

      TimeUnit.MILLISECONDS.sleep(messageRedeliveryDelay + 1000)
      // receive after redelivery, should have one message
      Get(s"/mom/receiveMessage/$queueName?timeOutSeconds=15") ~> momRoute ~> check {
        val response = new String(body.data.toByteArray)
        assertResult(OK)(status)
        val responseMsg: SimpleMessage = SimpleMessage.fromJson(response)
        messageUUIDList += responseMsg.deliveryAttemptID
        assertResult(responseMsg.contents)(messageContent)
      }

      // receive immediately again, should have no message
      Get(s"/mom/receiveMessage/$queueName?timeOutSeconds=2") ~> momRoute ~> check {
        val response = new String(body.data.toByteArray)
        assertResult(NoContent)(status)
        assertResult(s"No current Message available in queue $queueName!")(response)
      }

      val messageUUID = messageUUIDList(0)
      Put("/mom/acknowledge", HttpEntity(s"$messageUUID")) ~> momRoute ~> check {
        assertResult(OK)(status)
      }

      val nonExistingUUID = UUID.randomUUID()
      Put("/mom/acknowledge", HttpEntity(s"$nonExistingUUID")) ~> momRoute ~> check {
        val response = new String(body.data.toByteArray)
        assertResult(UnprocessableEntity)(status)
        assertResult(MessageDoesNotExistAndCannotBeCompletedException(nonExistingUUID).getMessage)(response)
      }

      // receive after complete, should have no message
      Get(s"/mom/receiveMessage/$queueName?timeOutSeconds=2") ~> momRoute ~> check {
        val response = new String(body.data.toByteArray)
        assertResult(NoContent)(status)
        assertResult(s"No current Message available in queue $queueName!")(response)
      }

      Put(s"/mom/deleteQueue/$queueName") ~> momRoute ~> check {
        assertResult(OK)(status)
      }
    }
  }

  "MessageQueueWebApi" should "respond 404 NotFound when " +
    "failures occur while creating/deleting a non-exiting queue, sending/receiving messages to a non-existing queue" in {

    ConfigSource.atomicConfig.configForBlock("shrine.messagequeue.blockingqWebApi.enabled", "true", "MessageQueueWebApiTest") {
      val doesNotExistQueue: String = "DoesNotExist"
      //todo Not a problem. The system is in the state you want. SHRINE-2308
      Put(s"/mom/deleteQueue/$doesNotExistQueue") ~> momRoute ~> check {
        assertResult(UnprocessableEntity)(status)
      }

      Put(s"/mom/sendMessage/$doesNotExistQueue", HttpEntity(s"$messageContent")) ~> momRoute ~> check {
        assertResult(UnprocessableEntity)(status)
      }

      // given timeout is 1 seconds
      Get(s"/mom/receiveMessage/$doesNotExistQueue?timeOutSeconds=1") ~> momRoute ~> check {
        assertResult(UnprocessableEntity)(status)
      }
    }
  }
}