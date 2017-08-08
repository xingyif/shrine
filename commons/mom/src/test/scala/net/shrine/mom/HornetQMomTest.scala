package net.shrine.mom

import net.shrine.mom.HornetQMom.Message
import org.junit.runner.RunWith
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.junit.JUnitRunner
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, FlatSpec, Matchers}

import scala.concurrent.duration._
import scala.language.postfixOps

/**
  * Test creation, insertion, querying, and deletion of ProblemDigest values into an
  * in-memory H2 database. Demonstrates proof of concept mapping of ProblemDigest
  * case class into a database.
  */
@RunWith(classOf[JUnitRunner])
class HornetQMomTest extends FlatSpec with BeforeAndAfterAll with ScalaFutures with Matchers {

  "HornetQ" should "be able to send and receive just one message" in {

    val queueName = "testQueue"

    assert(HornetQMom.queues.isEmpty)

    val queue = HornetQMom.createQueueIfAbsent(queueName)

    assert(HornetQMom.queues == Seq(queue))

    val testContents = "Test message"
    HornetQMom.send(testContents,queue)

    val message: Option[Message] = HornetQMom.receive(queue,1 second)

    assert(message.isDefined)
    assert(message.get.contents == testContents)

    message.fold()(HornetQMom.complete)

    val shouldBeNoMessage: Option[Message] = HornetQMom.receive(queue,1 second)

    assert(shouldBeNoMessage.isEmpty)

    HornetQMom.deleteQueue(queueName)
    assert(HornetQMom.queues.isEmpty)

  }

  "HornetQ" should "be able to send and receive a few messages" in {

    val queueName = "testQueue"

    assert(HornetQMom.queues.isEmpty)

    val queue = HornetQMom.createQueueIfAbsent(queueName)

    assert(HornetQMom.queues == Seq(queue))

    val testContents1 = "Test message1"
    HornetQMom.send(testContents1,queue)

    val message1: Option[Message] = HornetQMom.receive(queue,1 second)

    assert(message1.isDefined)
    assert(message1.get.contents == testContents1)

    message1.fold()(HornetQMom.complete)

    val shouldBeNoMessage1: Option[Message] = HornetQMom.receive(queue,1 second)

    assert(shouldBeNoMessage1.isEmpty)

    val testContents2 = "Test message2"
    HornetQMom.send(testContents2,queue)

    val testContents3 = "Test message3"
    HornetQMom.send(testContents3,queue)

    val message2: Option[Message] = HornetQMom.receive(queue,1 second)

    assert(message2.isDefined)
    assert(message2.get.contents == testContents2)

    message2.fold()(HornetQMom.complete)

    val message3: Option[Message] = HornetQMom.receive(queue,1 second)

    assert(message3.isDefined)
    assert(message3.get.contents == testContents3)

    message3.fold()(HornetQMom.complete)

    val shouldBeNoMessage4: Option[Message] = HornetQMom.receive(queue,1 second)

    assert(shouldBeNoMessage4.isEmpty)

    HornetQMom.deleteQueue(queueName)
    assert(HornetQMom.queues.isEmpty)

  }

  "HornetQ" should "be OK if asked to create the same queue twice " in {

    val queueName = "testQueue"
    HornetQMom.createQueueIfAbsent(queueName)
    HornetQMom.createQueueIfAbsent(queueName)

    HornetQMom.deleteQueue(queueName)
  }

  override def afterAll() = HornetQMomStopper.stop()
}
