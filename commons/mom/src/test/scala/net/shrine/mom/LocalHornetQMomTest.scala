package net.shrine.mom

import org.junit.runner.RunWith
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.junit.JUnitRunner
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}
import scala.collection.immutable.Seq
import scala.concurrent.duration._
import scala.language.postfixOps
/**
  * Test create, delete queue, send, and receive message, getQueueNames, and acknoledge using HornetQ service
  */
@RunWith(classOf[JUnitRunner])
class LocalHornetQMomTest extends FlatSpec with BeforeAndAfterAll with ScalaFutures with Matchers {

  "HornetQ" should "be able to send and receive just one message" in {

    val queueName = "testQueue"

    assert(LocalHornetQMom.queues.isEmpty)

    val queue = LocalHornetQMom.createQueueIfAbsent(queueName)

    assert(LocalHornetQMom.queues == Seq(queue))

    val testContents = "Test message"
    LocalHornetQMom.send(testContents,queue)

    val message: Option[Message] = LocalHornetQMom.receive(queue,1 second)

    assert(message.isDefined)
    assert(message.get.contents == testContents)

    LocalHornetQMom.completeMessage(message.get)

    val shouldBeNoMessage: Option[Message] = LocalHornetQMom.receive(queue,1 second)

    assert(shouldBeNoMessage.isEmpty)

    LocalHornetQMom.deleteQueue(queueName)
    assert(LocalHornetQMom.queues.isEmpty)
  }

  "HornetQ" should "be able to send and receive a few messages" in {

    val queueName = "testQueue"

    assert(LocalHornetQMom.queues.isEmpty)

    val queue = LocalHornetQMom.createQueueIfAbsent(queueName)

    assert(LocalHornetQMom.queues == Seq(queue))

    val testContents1 = "Test message1"
    LocalHornetQMom.send(testContents1,queue)

    val message1: Option[Message] = LocalHornetQMom.receive(queue,1 second)

    assert(message1.isDefined)
    assert(message1.get.contents == testContents1)

    LocalHornetQMom.completeMessage(message1.get)

    val shouldBeNoMessage1: Option[Message] = LocalHornetQMom.receive(queue,1 second)

    assert(shouldBeNoMessage1.isEmpty)

    val testContents2 = "Test message2"
    LocalHornetQMom.send(testContents2,queue)

    val testContents3 = "Test message3"
    LocalHornetQMom.send(testContents3,queue)

    val message2: Option[Message] = LocalHornetQMom.receive(queue,1 second)

    assert(message2.isDefined)
    assert(message2.get.contents == testContents2)

    LocalHornetQMom.completeMessage(message2.get)

    val message3: Option[Message] = LocalHornetQMom.receive(queue,1 second)

    assert(message3.isDefined)
    assert(message3.get.contents == testContents3)

    LocalHornetQMom.completeMessage(message3.get)

    val shouldBeNoMessage4: Option[Message] = LocalHornetQMom.receive(queue,1 second)

    assert(shouldBeNoMessage4.isEmpty)

    LocalHornetQMom.deleteQueue(queueName)
    assert(LocalHornetQMom.queues.isEmpty)
  }

  "HornetQ" should "be OK if asked to create the same queue twice " in {

    val queueName = "testQueue"
    LocalHornetQMom.createQueueIfAbsent(queueName)
    LocalHornetQMom.createQueueIfAbsent(queueName)

    LocalHornetQMom.deleteQueue(queueName)
  }

  override def afterAll() = LocalHornetQMomStopper.stop()
}