package net.shrine.hornetqmom
import java.util.UUID

import akka.actor.ActorRefFactory
import net.shrine.hornetqmom.LocalHornetQMom.SimpleMessage
import net.shrine.messagequeueservice.Queue
import net.shrine.source.ConfigSource
import org.json4s.NoTypeHints
import org.json4s.native.Serialization
import org.json4s.native.Serialization.read
import org.junit.runner.RunWith
import org.scalatest.FlatSpec
import org.scalatest.junit.JUnitRunner
import spray.http.HttpEntity
import spray.http.StatusCodes._
import spray.testkit.ScalatestRouteTest

import scala.collection.immutable.Seq
import scala.collection.mutable.ArrayBuffer
/**
  * Test basic functions of HornetQMomWebApi
  * Created by yifan on 7/27/17.
  */


@RunWith(classOf[JUnitRunner])
class HornetQMomWebApiTest extends FlatSpec with ScalatestRouteTest with HornetQMomWebApi {
  override def actorRefFactory: ActorRefFactory = system

  private val proposedQueueName = "test QueueInWebApi"
  private val queue: Queue = Queue(proposedQueueName)
  private val queueName: String = queue.name // "testQueue"
  private val messageContent = "test Content"

  "HornetQMomWebApi" should "create/delete the given queue, send/receive message, get queues" in {
    val configMap: Map[String, String] = Map( "shrine.messagequeue.blockingqWebApi.enabled" -> "true",
      "shrine.messagequeue.blockingq.messageTimeToLive" -> "2 days",
      "shrine.messagequeue.blockingq.messageRedeliveryDelay" -> "1 day",
      "shrine.messagequeue.blockingq.messageMaxDeliveryAttempts" -> "2")

    ConfigSource.atomicConfig.configForBlock(configMap, "HornetQMomWebApiTest") {

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

      val messageUUID = messageUUIDList(0)
      Put("/mom/acknowledge", HttpEntity(s"$messageUUID")) ~> momRoute ~> check {
        assertResult(ResetContent)(status)
      }

      val nonExistingUUID = UUID.randomUUID()
      Put("/mom/acknowledge", HttpEntity(s"$nonExistingUUID")) ~> momRoute ~> check {
        val response = new String(body.data.toByteArray)
        assertResult(NotFound)(status)
        assertResult(MessageDoesNotExistAndCannotBeCompletedException(nonExistingUUID).getMessage)(response)
      }

      Put(s"/mom/deleteQueue/$queueName") ~> momRoute ~> check {
        assertResult(OK)(status)
      }
    }
  }

  "HornetQMomWebApi" should "respond InternalServerError with the corresponding error message when " +
    "failures occur while creating/deleting the given queue, sending/receiving message, getting queues" in {

    ConfigSource.atomicConfig.configForBlock("shrine.messagequeue.blockingqWebApi.enabled", "true", "HornetQMomWebApiTest") {

      //todo Not a problem. The system is in the state you want. SHRINE-2308
      Put(s"/mom/deleteQueue/$queueName") ~> momRoute ~> check {
        assertResult(NotFound)(status)
      }

      Put(s"/mom/sendMessage/$queueName", HttpEntity(s"$messageContent")) ~> momRoute ~> check {
        assertResult(NotFound)(status)
      }

      // given timeout is 1 seconds
      Get(s"/mom/receiveMessage/$queueName?timeOutSeconds=1") ~> momRoute ~> check {
        assertResult(NotFound)(status)
      }
    }
  }
}