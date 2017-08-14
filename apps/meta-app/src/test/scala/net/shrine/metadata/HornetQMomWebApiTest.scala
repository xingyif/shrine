package net.shrine.metadata

import akka.actor.ActorRefFactory
import net.shrine.mom.{Message, MessageSerializer, Queue}
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

  private val queueName = "testQueue"
  private val messageContent = "testContent"
  private var receivedMessage: String = ""

  "HornetQMomWebApi" should "create/delete the given queue, send/receive message, get queues" in {

    Put(s"/mom/createQueue/$queueName") ~> momRoute ~> check {
      val response = new String(body.data.toByteArray)
      implicit val formats = Serialization.formats(NoTypeHints)
      val jsonToQueue = read[Queue](response)(formats, manifest[Queue])
      val responseQueueName = jsonToQueue.name
      assertResult(Created)(status)
      assertResult(queueName)(responseQueueName)
    }

    Put(s"/mom/sendMessage/$messageContent/$queueName") ~> momRoute ~> check {
      assertResult(Accepted)(status)
    }

    Get(s"/mom/getQueues") ~> momRoute ~> check {

      implicit val formats = Serialization.formats(NoTypeHints)
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

    Put("/mom/acknowledge", HttpEntity(s"""$receivedMessage""")) ~>
      momRoute ~> check {
      implicit val formats = Serialization.formats(NoTypeHints) + new MessageSerializer
      assertResult(ResetContent)(status)
    }

    Put(s"/mom/deleteQueue/$queueName") ~> momRoute ~> check {
      assertResult(OK)(status)
    }
  }
}