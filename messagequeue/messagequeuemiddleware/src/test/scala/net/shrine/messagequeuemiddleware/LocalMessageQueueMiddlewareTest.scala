package net.shrine.messagequeuemiddleware

import java.util.UUID
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
  * Test create, delete queue, send, and receive message, getQueueNames, and acknowledge using MessageQueue service
  */
@RunWith(classOf[JUnitRunner])
class LocalMessageQueueMiddlewareTest extends FlatSpec with BeforeAndAfterAll with ScalaFutures with Matchers {

  "BlockingQueue" should "be able to send and receive just one message" in {

    val configMap: Map[String, String] = Map("shrine.messagequeue.blockingq.messageTimeToLive" -> "5 seconds",
      "shrine.messagequeue.blockingq.messageRedeliveryDelay" -> "2 seconds",
      "shrine.messagequeue.blockingq.messageMaxDeliveryAttempts" -> "1")

    ConfigSource.atomicConfig.configForBlock(configMap, "LocalMessageQueueMiddlewareTest") {
      val queueName = "receiveOneMessage"

      val messageTimeToLive = ConfigSource.config.get("shrine.messagequeue.blockingq.messageTimeToLive", Duration(_)).toMillis
      val messageRedeliveryDelay = ConfigSource.config.get("shrine.messagequeue.blockingq.messageRedeliveryDelay", Duration(_)).toMillis

      assert(LocalMessageQueueMiddleware.queues.get.isEmpty)

      val queue = LocalMessageQueueMiddleware.createQueueIfAbsent(queueName).get

      assert(LocalMessageQueueMiddleware.queues.get == Seq(queue))

      val testContents = "Test message_receiveOneMessage"
      val sendTry = LocalMessageQueueMiddleware.send(testContents, queue)
      assert(sendTry.isSuccess)

      // first time receive
      val message: Option[Message] = LocalMessageQueueMiddleware.receive(queue, 1 second).get

      assert(message.isDefined)
      assert(message.get.contents == testContents)

      // receive immediately again, should be no message redelivered yet
      val sameMessage1: Option[Message] = LocalMessageQueueMiddleware.receive(queue, 1 second).get
      assert(sameMessage1.isEmpty)

      // receive after the redelivery delay, should have the redelivered message
      TimeUnit.MILLISECONDS.sleep(messageRedeliveryDelay + 1000)
      val sameMessage2: Option[Message] = LocalMessageQueueMiddleware.receive(queue, 1 second).get
      assert(sameMessage2.isDefined)
      assert(sameMessage2.get.contents == testContents)

      // receive after the redelivery delay again, reached maxRedelivery attempt, should be no message
      TimeUnit.MILLISECONDS.sleep(messageRedeliveryDelay + 1000)
      val sameMessage3: Option[Message] = LocalMessageQueueMiddleware.receive(queue, 1 second).get
      assert(sameMessage3.isEmpty)

      // call complete on the last Message
      val completeTry: Try[Unit] = sameMessage2.get.complete()
      assert(completeTry.isSuccess)
      // receive after message is completed, should be no message
      val shouldBeNoMessage: Option[Message] = LocalMessageQueueMiddleware.receive(queue, 1 second).get
      assert(shouldBeNoMessage.isEmpty)

      val queueName_ExpiredMessage = "QueueExpiredMessage"
      val queue_expiredMessage: Try[Queue] = LocalMessageQueueMiddleware.createQueueIfAbsent(queueName_ExpiredMessage)
      // message should expire
      val testContents2 = "Test message_MessageShouldExpire"
      val sendTry2 = LocalMessageQueueMiddleware.send(testContents2, queue_expiredMessage.get)
      assert(sendTry2.isSuccess)

      // first time receive
      val messageExpire: Option[Message] = LocalMessageQueueMiddleware.receive(queue_expiredMessage.get, 1 second).get
      assert(messageExpire.isDefined)
      assert(messageExpire.get.contents == testContents2)

      TimeUnit.MILLISECONDS.sleep(messageTimeToLive + 1000)
      val shouldBeNoMessageExpired: Option[Message] = LocalMessageQueueMiddleware.receive(queue_expiredMessage.get, 1 second).get
      assert(shouldBeNoMessageExpired.isEmpty)

      val deleteTry = LocalMessageQueueMiddleware.deleteQueue(queueName)
      val deleteExpiredMessageQueueTry = LocalMessageQueueMiddleware.deleteQueue(queueName_ExpiredMessage)
      assert(deleteTry.isSuccess)
      assert(deleteExpiredMessageQueueTry.isSuccess)
      assert(LocalMessageQueueMiddleware.queues.get.isEmpty)
    }
  }

  "BlockingQueue" should "be able to send and receive a few messages" in {

    val queueName = "receiveAFewMessages"

    assert(LocalMessageQueueMiddleware.queues.get.isEmpty)

    val queue = LocalMessageQueueMiddleware.createQueueIfAbsent(queueName).get

    assert(LocalMessageQueueMiddleware.queues.get == Seq(queue))

    val testContents1 = "First test message_receiveAFewMessages"
    val sendTry = LocalMessageQueueMiddleware.send(testContents1, queue)
    assert(sendTry.isSuccess)

    val message1: Option[Message] = LocalMessageQueueMiddleware.receive(queue, 1 second).get

    assert(message1.isDefined)
    assert(message1.get.contents == testContents1)

    val completeTry = message1.get.complete()
    assert(completeTry.isSuccess)

    val shouldBeNoMessage1: Option[Message] = LocalMessageQueueMiddleware.receive(queue, 1 second).get

    assert(shouldBeNoMessage1.isEmpty)

    val testContents2 = "Second test message_receiveAFewMessages"
    val sendTry2 = LocalMessageQueueMiddleware.send(testContents2, queue)
    assert(sendTry2.isSuccess)

    val testContents3 = "Third test message_receiveAFewMessages"
    val sendTry3 = LocalMessageQueueMiddleware.send(testContents3, queue)
    assert(sendTry3.isSuccess)

    val message2: Option[Message] = LocalMessageQueueMiddleware.receive(queue, 1 second).get

    assert(message2.isDefined)
    assert(message2.get.contents == testContents2)

    val completeTry2 = message2.get.complete()
    assert(completeTry2.isSuccess)

    val message3: Option[Message] = LocalMessageQueueMiddleware.receive(queue, 1 second).get

    assert(message3.isDefined)
    assert(message3.get.contents == testContents3)

    val completeTry3 = message3.get.complete()
    assert(completeTry3.isSuccess)

    val shouldBeNoMessage4: Option[Message] = LocalMessageQueueMiddleware.receive(queue, 1 second).get

    assert(shouldBeNoMessage4.isEmpty)

    val deleteTry = LocalMessageQueueMiddleware.deleteQueue(queueName)
    assert(deleteTry.isSuccess)
    assert(LocalMessageQueueMiddleware.queues.get.isEmpty)
  }

  "BlockingQueue" should "be OK if asked to create the same queue twice " in {

    val queueName = "createSameQueueTwice"
    val queue = LocalMessageQueueMiddleware.createQueueIfAbsent(queueName)
    assert(queue.isSuccess)
    val sameQueue = LocalMessageQueueMiddleware.createQueueIfAbsent(queueName)
    assert(sameQueue.isSuccess)

    assert(LocalMessageQueueMiddleware.queues.get == Seq(sameQueue.get))
    val deleteTry = LocalMessageQueueMiddleware.deleteQueue(queueName)
    assert(deleteTry.isSuccess)
    assert(LocalMessageQueueMiddleware.queues.get.isEmpty)
  }

  "BlockingQueue" should "return a failure if deleting a non-existing queue" in {

    val queueName = "DeletingNonExistingQueue"
    val deleteQueue = LocalMessageQueueMiddleware.deleteQueue(queueName)
    assert(deleteQueue.isFailure)
    assert(LocalMessageQueueMiddleware.queues.get.isEmpty)
  }

  "BlockingQueue" should "return a failure if sending message to a non-existing queue" in {

    val queueName = "SendToNonExistingQueue"
    val sendTry = LocalMessageQueueMiddleware.send("testContent", Queue(queueName))
    assert(sendTry.isFailure)
    assert(LocalMessageQueueMiddleware.queues.get.isEmpty)
  }

  "BlockingQueue" should "return a failure if receiving a message to a non-existing queue" in {

    val queueName = "ReceiveFromNonExistingQueue"
    val receiveTry = LocalMessageQueueMiddleware.receive(Queue(queueName), Duration(1, "second"))
    assert(receiveTry.isFailure)
    assert(LocalMessageQueueMiddleware.queues.get.isEmpty)
  }

  "BlockingQueue" should "be able to filter the special characters in queue name" in {

    val queueName = "test# Qu%eueFilter"

    assert(Queue(queueName).name == "testQueueFilter")
  }

  "BlockingQueue" should "make sure if two InternalMessages are equal, then they have the same hashCode" in {
    val id: UUID = UUID.randomUUID()
    val createdTime = System.currentTimeMillis()
    val message1: InternalMessage = InternalMessage(id, "message1", createdTime, Queue("to"), 0)
    val message2: InternalMessage = InternalMessage(id, "message1", createdTime, Queue("to"), 1)
    assert(message1 == message2)
    val message3: InternalMessage = InternalMessage(UUID.randomUUID(), "message3", createdTime, Queue("to"), 0)
    assert(message1 != message3)
  }
}