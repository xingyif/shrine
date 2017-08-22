package net.shrine.hornetqmom

import akka.actor.ActorRefFactory
import net.shrine.messagequeueservice.{Message, MessageSerializer, Queue, QueueSerializer}
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
/**
  * Created by yifan on 7/27/17.
  */


@RunWith(classOf[JUnitRunner])
class HornetQMomWebApiTest extends FlatSpec with ScalatestRouteTest with HornetQMomWebApi {
  override def actorRefFactory: ActorRefFactory = system

  private val proposedQueueName = "test Queue"
  private val queue: Queue = Queue(proposedQueueName)
  private val queueName: String = queue.name // "testQueue"
  private val messageContent = "test Content"
  private var receivedMessage: String = ""

  "HornetQMomWebApi" should "create/delete the given queue, send/receive message, get queues" in {

    Put(s"/mom/createQueue/$queueName") ~> momRoute ~> check {
      val response = new String(body.data.toByteArray)
      implicit val formats = Serialization.formats(NoTypeHints) + new QueueSerializer
      val jsonToQueue = read[Queue](response)(formats, manifest[Queue])
      val responseQueueName = jsonToQueue.name
      assertResult(Created)(status)
      assertResult(queueName)(responseQueueName)
    }

    // should be OK to create a queue twice
    Put(s"/mom/createQueue/$queueName") ~> momRoute ~> check {
      val response = new String(body.data.toByteArray)
      implicit val formats = Serialization.formats(NoTypeHints) + new QueueSerializer
      val jsonToQueue = read[Queue](response)(formats, manifest[Queue])
      val responseQueueName = jsonToQueue.name
      assertResult(Created)(status)
      assertResult(queueName)(responseQueueName)
    }

    Put(s"/mom/sendMessage/$queueName", HttpEntity(s"$messageContent")) ~> momRoute ~> check {
      assertResult(Accepted)(status)
    }

    Get(s"/mom/getQueues") ~> momRoute ~> check {

      implicit val formats = Serialization.formats(NoTypeHints) + new QueueSerializer
      val response: String = new String(body.data.toByteArray)
      val jsonToSeq: Seq[Queue] = read[Seq[Queue]](response, false)(formats, manifest[Seq[Queue]])

      assertResult(OK)(status)
      assertResult(queueName)(jsonToSeq.head.name)
    }

    // given timeout is 2 seconds
    Get(s"/mom/receiveMessage/$queueName?timeOutSeconds=2") ~> momRoute ~> check {
      val response = new String(body.data.toByteArray)
      receivedMessage = response

      implicit val formats = Serialization.formats(NoTypeHints) + new MessageSerializer
      val responseToMessage: Message = read[Message](response)(formats, manifest[Message])

      assertResult(OK)(status)
      assert(responseToMessage.isInstanceOf[Message])
    }

    Put("/mom/acknowledge", HttpEntity(s"$receivedMessage")) ~>
      momRoute ~> check {
      implicit val formats = Serialization.formats(NoTypeHints) + new MessageSerializer
      assertResult(ResetContent)(status)
    }

    Put(s"/mom/deleteQueue/$queueName") ~> momRoute ~> check {
      assertResult(OK)(status)
    }
  }

  "HornetQMomWebApi" should "respond Internal server error with the corresponding error message when " +
    "failures occur while creating/deleting the given queue, sending/receiving message, getting queues" in {

    Put(s"/mom/deleteQueue/$queueName") ~> momRoute ~> check {
      assertResult(InternalServerError)(status)
    }

    Put(s"/mom/sendMessage/$queueName", HttpEntity(s"$messageContent")) ~> momRoute ~> check {
      assertResult(InternalServerError)(status)
    }

    // given timeout is 1 seconds
    Get(s"/mom/receiveMessage/$queueName?timeOutSeconds=1") ~> momRoute ~> check {
      assertResult(InternalServerError)(status)
    }
  }
}

@RunWith(classOf[JUnitRunner])
class HornetQMomWebApiConfigTest extends FlatSpec with ScalatestRouteTest with HornetQMomWebApi {
  override def actorRefFactory: ActorRefFactory = system

  private val queueName = "testQueue"


  override val enabled: Boolean = ConfigSource.config.getString("shrine.messagequeue.hornetQWebApiTest.enabled").toBoolean

  "HornetQMomWebApi" should "block user from using the API and return a 404 response" in {

    Put(s"/mom/createQueue/$queueName") ~> momRoute ~> check {
      val response = new String(body.data.toByteArray)

      assertResult("HornetQWebApi needs to be enabled before use!")(response)
      assertResult(NotFound)(status)
    }
  }
}