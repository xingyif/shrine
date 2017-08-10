package net.shrine.metadata

import akka.actor.ActorRefFactory
import net.shrine.metadata.{HornetQMomWebApi, HornetQMomWebClient}
import net.shrine.mom.Message
import org.junit.runner.RunWith
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}
import org.scalatest.junit.JUnitRunner
import spray.testkit.ScalatestRouteTest

import scala.concurrent.duration._
import scala.collection.immutable.Seq

/**
  * Test create, delete queue, send, and receive message, getQueueNames, and acknowledge using HornetQMomWebClient
  *  Created by yifan on 8/10/17.
  */

@RunWith(classOf[JUnitRunner])
class HornetQMomWebClientTest extends FlatSpec with BeforeAndAfterAll with ScalaFutures with Matchers {

  //  override def actorRefFactory: ActorRefFactory = system

  "HornetQMomWebClient" should "be able to send and receive just one message" in {

    val queueName = "testQueue"

    assert(HornetQMomWebClient.queues.isEmpty)


    val queue = HornetQMomWebClient.createQueueIfAbsent(queueName)

    assert(HornetQMomWebClient.queues == Seq(queue))

    val testContents = "Test message"
    HornetQMomWebClient.send(testContents,queue)

    val message: Option[Message] = HornetQMomWebClient.receive(queue,1 second)

    assert(message.isDefined)
    assert(message.get.contents == testContents)


    HornetQMomWebClient.completeMessage(message.get)

    val shouldBeNoMessage: Option[Message] = HornetQMomWebClient.receive(queue,1 second)

    assert(shouldBeNoMessage.isEmpty)

    HornetQMomWebClient.deleteQueue(queueName)
    assert(HornetQMomWebClient.queues.isEmpty)
  }

  "HornetQ" should "be able to send and receive a few messages" in {

    val queueName = "testQueue"

    assert(HornetQMomWebClient.queues.isEmpty)

    val queue = HornetQMomWebClient.createQueueIfAbsent(queueName)

    assert(HornetQMomWebClient.queues == Seq(queue))

    val testContents1 = "Test message1"
    HornetQMomWebClient.send(testContents1,queue)

    val message1: Option[Message] = HornetQMomWebClient.receive(queue,1 second)

    assert(message1.isDefined)
    assert(message1.get.contents == testContents1)

    HornetQMomWebClient.completeMessage(message1.get)

    val shouldBeNoMessage1: Option[Message] = HornetQMomWebClient.receive(queue,1 second)

    assert(shouldBeNoMessage1.isEmpty)

    val testContents2 = "Test message2"
    HornetQMomWebClient.send(testContents2,queue)

    val testContents3 = "Test message3"
    HornetQMomWebClient.send(testContents3,queue)

    val message2: Option[Message] = HornetQMomWebClient.receive(queue,1 second)

    assert(message2.isDefined)
    assert(message2.get.contents == testContents2)

    HornetQMomWebClient.completeMessage(message2.get)

    val message3: Option[Message] = HornetQMomWebClient.receive(queue,1 second)

    assert(message3.isDefined)
    assert(message3.get.contents == testContents3)

    HornetQMomWebClient.completeMessage(message3.get)

    val shouldBeNoMessage4: Option[Message] = HornetQMomWebClient.receive(queue,1 second)

    assert(shouldBeNoMessage4.isEmpty)

    HornetQMomWebClient.deleteQueue(queueName)
    assert(HornetQMomWebClient.queues.isEmpty)
  }

  "HornetQ" should "be OK if asked to create the same queue twice " in {

    val queueName = "testQueue"
    HornetQMomWebClient.createQueueIfAbsent(queueName)
    HornetQMomWebClient.createQueueIfAbsent(queueName)

    HornetQMomWebClient.deleteQueue(queueName)
  }
}