package net.shrine.hornetqmom

import net.shrine.messagequeueservice.Queue
import org.junit.runner.RunWith
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.junit.JUnitRunner
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}

import scala.collection.immutable.Seq
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Try
/**
  * Test create, delete queue, send, and receive message, getQueueNames, and acknoledge using HornetQ service
  */
@RunWith(classOf[JUnitRunner])
class LocalHornetQMomTest extends FlatSpec with BeforeAndAfterAll with ScalaFutures with Matchers {

  "HornetQ" should "be able to send and receive just one message" in {

    val queueName = "testQueue"

    assert(LocalHornetQMom.queues.get.isEmpty)

    val queue = LocalHornetQMom.createQueueIfAbsent(queueName).get

    assert(LocalHornetQMom.queues.get == Seq(queue))

    val testContents = "Test message"
    val sendTry = LocalHornetQMom.send(testContents, queue)
    assert(sendTry.isSuccess)

    val message: Option[LocalHornetQMom.LocalHornetQMessage] = LocalHornetQMom.receive(queue,1 second).get

    assert(message.isDefined)
    assert(message.get.getContents == testContents)

    val completeTry: Try[Unit] = message.get.complete()
    assert(completeTry.isSuccess)

    val shouldBeNoMessage: Option[LocalHornetQMom.LocalHornetQMessage] = LocalHornetQMom.receive(queue,1 second).get

    assert(shouldBeNoMessage.isEmpty)

    val deleteTry = LocalHornetQMom.deleteQueue(queueName)
    assert(deleteTry.isSuccess)
    assert(LocalHornetQMom.queues.get.isEmpty)
  }

  "HornetQ" should "be able to send and receive a few messages" in {

    val queueName = "testQueue"

    assert(LocalHornetQMom.queues.get.isEmpty)

    val queue = LocalHornetQMom.createQueueIfAbsent(queueName).get

    assert(LocalHornetQMom.queues.get == Seq(queue))

    val testContents1 = "Test message1"
    val sendTry = LocalHornetQMom.send(testContents1,queue)
    assert(sendTry.isSuccess)

    val message1: Option[LocalHornetQMom.LocalHornetQMessage] = LocalHornetQMom.receive(queue,1 second).get

    assert(message1.isDefined)
    assert(message1.get.getContents == testContents1)

    val completeTry = message1.get.complete()
    assert(completeTry.isSuccess)

    val shouldBeNoMessage1: Option[LocalHornetQMom.LocalHornetQMessage] = LocalHornetQMom.receive(queue,1 second).get

    assert(shouldBeNoMessage1.isEmpty)

    val testContents2 = "Test message2"
    val sendTry2 = LocalHornetQMom.send(testContents2,queue)
    assert(sendTry2.isSuccess)

    val testContents3 = "Test message3"
    val sendTry3 = LocalHornetQMom.send(testContents3,queue)
    assert(sendTry3.isSuccess)

    val message2: Option[LocalHornetQMom.LocalHornetQMessage] = LocalHornetQMom.receive(queue,1 second).get

    assert(message2.isDefined)
    assert(message2.get.getContents == testContents2)

    val completeTry2 = message2.get.complete()
    assert(completeTry2.isSuccess)

    val message3: Option[LocalHornetQMom.LocalHornetQMessage] = LocalHornetQMom.receive(queue,1 second).get

    assert(message3.isDefined)
    assert(message3.get.getContents == testContents3)

    val completeTry3 = message3.get.complete()
    assert(completeTry3.isSuccess)

    val shouldBeNoMessage4: Option[LocalHornetQMom.LocalHornetQMessage] = LocalHornetQMom.receive(queue,1 second).get

    assert(shouldBeNoMessage4.isEmpty)

    val deleteTry = LocalHornetQMom.deleteQueue(queueName)
    assert(deleteTry.isSuccess)
    assert(LocalHornetQMom.queues.get.isEmpty)
  }

  "HornetQ" should "be OK if asked to create the same queue twice " in {

    val queueName = "testQueue"
    val queue = LocalHornetQMom.createQueueIfAbsent(queueName)
    assert(queue.isSuccess)
    val sameQueue = LocalHornetQMom.createQueueIfAbsent(queueName)
    assert(sameQueue.isSuccess)

    assert(LocalHornetQMom.queues.get == Seq(sameQueue.get))
    val deleteTry = LocalHornetQMom.deleteQueue(queueName)
    assert(deleteTry.isSuccess)
    assert(LocalHornetQMom.queues.get.isEmpty)
  }

  "HornetQ" should "return a failure if deleting a non-existing queue" in {

    val queueName = "testQueue"
    val deleteQueue = LocalHornetQMom.deleteQueue(queueName)
    assert(deleteQueue.isFailure)
  }

  "HornetQ" should "return a failure if sending message to a non-existing queue" in {

    val queueName = "non-existingQueue"
    val sendTry = LocalHornetQMom.send("testContent", Queue(queueName))
    assert(sendTry.isFailure)
  }

  "HornetQ" should "return a failure if receiving a message to a non-existing queue" in {

    val queueName = "non-existingQueue"
    val receiveTry = LocalHornetQMom.receive(Queue(queueName), Duration(1, "second"))
    assert(receiveTry.isFailure)
  }

  "HornetQ" should "be able to filter the special characters in queue name" in {

    val queueName = "test# Qu%eueFilter"

    assert(LocalHornetQMom.queues.get.isEmpty)

    val queue = LocalHornetQMom.createQueueIfAbsent(queueName).get

    assert(LocalHornetQMom.queues.get == Seq(queue))
    LocalHornetQMom.deleteQueue(queue.name)
  }


  override def afterAll() = LocalHornetQMomStopper.stop()
}