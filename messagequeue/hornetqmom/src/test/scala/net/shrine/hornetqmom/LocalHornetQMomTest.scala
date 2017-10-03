package net.shrine.hornetqmom

import java.util.concurrent.TimeUnit
import net.shrine.config.ConfigExtensions
import net.shrine.messagequeueservice.{Message, Queue}
import net.shrine.source.ConfigSource
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

  "BlockingQueue" should "be able to send and receive just one message" in {

    val configMap: Map[String, String] = Map("shrine.messagequeue.blockingq.messageTimeToLive" -> "5 seconds",
      "shrine.messagequeue.blockingq.messageRedeliveryDelay" -> "2 seconds",
      "shrine.messagequeue.blockingq.messageMaxDeliveryAttempts" -> "2")

    ConfigSource.atomicConfig.configForBlock(configMap, "LocalHornetQMomTest") {
      val queueName = "receiveOneMessage"

      val messageTimeToLive = ConfigSource.config.get("shrine.messagequeue.blockingq.messageTimeToLive", Duration(_)).toMillis
      val messageRedeliveryDelay = ConfigSource.config.get("shrine.messagequeue.blockingq.messageRedeliveryDelay", Duration(_)).toMillis

      assert(LocalHornetQMom.queues.get.isEmpty)

      val queue = LocalHornetQMom.createQueueIfAbsent(queueName).get

      assert(LocalHornetQMom.queues.get == Seq(queue))

      val testContents = "Test message_receiveOneMessage"
      val sendTry = LocalHornetQMom.send(testContents, queue)
      assert(sendTry.isSuccess)

      // first time receive
      val message: Option[Message] = LocalHornetQMom.receive(queue, 1 second).get

      assert(message.isDefined)
      assert(message.get.contents == testContents)

      // receive immediately again, should be no message redelivered yet
      val sameMessage1: Option[Message] = LocalHornetQMom.receive(queue, 1 second).get
      assert(sameMessage1.isEmpty)

      // receive after the redelivery delay, should have the redelivered message
      TimeUnit.MILLISECONDS.sleep(messageRedeliveryDelay + 1000)
      val sameMessage2: Option[Message] = LocalHornetQMom.receive(queue, 1 second).get
      assert(sameMessage2.isDefined)
      assert(sameMessage2.get.contents == testContents)

      // receive after the redelivery delay again, reached maxRedelivery attempt, should be no message
      TimeUnit.MILLISECONDS.sleep(messageRedeliveryDelay + 1000)
      val sameMessage3: Option[Message] = LocalHornetQMom.receive(queue, 1 second).get
      assert(sameMessage3.isEmpty)

      val completeTry: Try[Unit] = message.get.complete()
      assert(completeTry.isSuccess)
      // receive after message is completed, should be no message
      val shouldBeNoMessage: Option[Message] = LocalHornetQMom.receive(queue, 1 second).get

      assert(shouldBeNoMessage.isEmpty)

      // message should expire
      val testContents2 = "Test message_MessageShouldExpire"
      val sendTry2 = LocalHornetQMom.send(testContents2, queue)
      assert(sendTry2.isSuccess)

      // first time receive
      val messageExpire: Option[Message] = LocalHornetQMom.receive(queue, 1 second).get
      assert(messageExpire.isDefined)
      assert(messageExpire.get.contents == testContents2)

      TimeUnit.MILLISECONDS.sleep(messageTimeToLive + 1000)
      val shouldBeNoMessageExpired: Option[Message] = LocalHornetQMom.receive(queue, 1 second).get
      assert(shouldBeNoMessageExpired.isEmpty)

      val deleteTry = LocalHornetQMom.deleteQueue(queueName)
      assert(deleteTry.isSuccess)
      assert(LocalHornetQMom.queues.get.isEmpty)
    }
  }

  "BlockingQueue" should "be able to send and receive a few messages" in {

    val queueName = "receiveAFewMessages"

    assert(LocalHornetQMom.queues.get.isEmpty)

    val queue = LocalHornetQMom.createQueueIfAbsent(queueName).get

    assert(LocalHornetQMom.queues.get == Seq(queue))

    val testContents1 = "First test message_receiveAFewMessages"
    val sendTry = LocalHornetQMom.send(testContents1, queue)
    assert(sendTry.isSuccess)

    val message1: Option[Message] = LocalHornetQMom.receive(queue, 1 second).get

    assert(message1.isDefined)
    assert(message1.get.contents == testContents1)

    val completeTry = message1.get.complete()
    assert(completeTry.isSuccess)

    val shouldBeNoMessage1: Option[Message] = LocalHornetQMom.receive(queue, 1 second).get

    assert(shouldBeNoMessage1.isEmpty)

    val testContents2 = "Second test message_receiveAFewMessages"
    val sendTry2 = LocalHornetQMom.send(testContents2, queue)
    assert(sendTry2.isSuccess)

    val testContents3 = "Third test message_receiveAFewMessages"
    val sendTry3 = LocalHornetQMom.send(testContents3, queue)
    assert(sendTry3.isSuccess)

    val message2: Option[Message] = LocalHornetQMom.receive(queue, 1 second).get

    assert(message2.isDefined)
    assert(message2.get.contents == testContents2)

    val completeTry2 = message2.get.complete()
    assert(completeTry2.isSuccess)

    val message3: Option[Message] = LocalHornetQMom.receive(queue, 1 second).get

    assert(message3.isDefined)
    assert(message3.get.contents == testContents3)

    val completeTry3 = message3.get.complete()
    assert(completeTry3.isSuccess)

    val shouldBeNoMessage4: Option[Message] = LocalHornetQMom.receive(queue, 1 second).get

    assert(shouldBeNoMessage4.isEmpty)

    val deleteTry = LocalHornetQMom.deleteQueue(queueName)
    assert(deleteTry.isSuccess)
    assert(LocalHornetQMom.queues.get.isEmpty)
  }

  "BlockingQueue" should "be OK if asked to create the same queue twice " in {

    val queueName = "createSameQueueTwice"
    val queue = LocalHornetQMom.createQueueIfAbsent(queueName)
    assert(queue.isSuccess)
    val sameQueue = LocalHornetQMom.createQueueIfAbsent(queueName)
    assert(sameQueue.isSuccess)

    assert(LocalHornetQMom.queues.get == Seq(sameQueue.get))
    val deleteTry = LocalHornetQMom.deleteQueue(queueName)
    assert(deleteTry.isSuccess)
    assert(LocalHornetQMom.queues.get.isEmpty)
  }

  "BlockingQueue" should "return a failure if deleting a non-existing queue" in {

    val queueName = "DeletingNonExistingQueue"
    val deleteQueue = LocalHornetQMom.deleteQueue(queueName)
    assert(deleteQueue.isFailure)
  }

  "BlockingQueue" should "return a failure if sending message to a non-existing queue" in {

    val queueName = "SendToNonExistingQueue"
    val sendTry = LocalHornetQMom.send("testContent", Queue(queueName))
    assert(sendTry.isFailure)
  }

  "BlockingQueue" should "return a failure if receiving a message to a non-existing queue" in {

    val queueName = "ReceiveFromNonExistingQueue"
    val receiveTry = LocalHornetQMom.receive(Queue(queueName), Duration(1, "second"))
    assert(receiveTry.isFailure)
  }

  "BlockingQueue" should "be able to filter the special characters in queue name" in {

    val queueName = "test# Qu%eueFilter"

    assert(Queue(queueName).name == "testQueueFilter")
  }

  override def afterAll() = LocalHornetQMomStopper.stop()
}