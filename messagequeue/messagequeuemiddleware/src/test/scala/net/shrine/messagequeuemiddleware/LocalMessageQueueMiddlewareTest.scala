//package net.shrine.messagequeuemiddleware
//
//import java.util.UUID
//import java.util.concurrent.TimeUnit
//
//import net.shrine.config.ConfigExtensions
//import net.shrine.messagequeueservice.{Message, Queue}
//import net.shrine.source.ConfigSource
//import org.junit.runner.RunWith
//import org.scalatest.concurrent.ScalaFutures
//import org.scalatest.concurrent.AsyncAssertions
//import org.scalatest.junit.JUnitRunner
//import org.scalatest.time.{Millis, Seconds, Span}
//import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}
//
//import scala.collection.immutable.Seq
//import scala.concurrent.{Await, Future}
//import scala.concurrent.duration._
//import scala.language.postfixOps
//import scala.util.Try
///**
//  * Test create, delete queue, send, and receive message, getQueueNames, and acknowledge using MessageQueue service
//  */
//@RunWith(classOf[JUnitRunner])
//class LocalMessageQueueMiddlewareTest extends FlatSpec with BeforeAndAfterAll with ScalaFutures with Matchers {
//
//  val waitForFutureToComplete = Duration("1 minute")
//  implicit val defaultPatience = PatienceConfig(timeout = Span(5, Seconds), interval = Span(500, Millis))
//
//  "BlockingQueue" should "be able to send and receive just one message" in {
//
//    val configMap: Map[String, String] = Map("shrine.messagequeue.blockingq.messageTimeToLive" -> "7 seconds",
//      "shrine.messagequeue.blockingq.messageRedeliveryDelay" -> "2 seconds",
//      "shrine.messagequeue.blockingq.messageMaxDeliveryAttempts" -> "1")
//
//    ConfigSource.atomicConfig.configForBlock(configMap, "LocalMessageQueueMiddlewareTest") {
//      val queueName = "receiveOneMessage"
//
//      val messageTimeToLive = ConfigSource.config.get("shrine.messagequeue.blockingq.messageTimeToLive", Duration(_)).toMillis
//      val messageRedeliveryDelay = ConfigSource.config.get("shrine.messagequeue.blockingq.messageRedeliveryDelay", Duration(_)).toMillis
//
//      assert(LocalMessageQueueMiddleware.queues.get.isEmpty)
//
//      val queue = LocalMessageQueueMiddleware.createQueueIfAbsent(queueName).get
//
//      assert(LocalMessageQueueMiddleware.queues.get == Seq(queue))
//
//      val testContents = "Test message_receiveOneMessage"
//      Await.result(LocalMessageQueueMiddleware.send(testContents, queue), waitForFutureToComplete)
//
//      // first time receive
//      val message: Option[Message] = whenReady(LocalMessageQueueMiddleware.receive(queue, 1 second)) { messageOpt =>
//        assert(messageOpt.isDefined)
//        assert(messageOpt.get.contents == testContents)
//        messageOpt
//      }
//
//
//      // receive immediately again, should be no message redelivered yet
//      whenReady(LocalMessageQueueMiddleware.receive(queue, 1 second)) { noMessage =>
//        assert(noMessage.isEmpty)
//      }
//
//      // receive after the redelivery delay, should have the redelivered message
//      TimeUnit.MILLISECONDS.sleep(messageRedeliveryDelay + 1000)
//      whenReady(LocalMessageQueueMiddleware.receive(queue, 2 second)) { sameMessage =>
//        assert(sameMessage.isDefined)
//        assert(sameMessage.get.contents == testContents)
//      }
//
//
//      // receive after the redelivery delay again, reached maxRedelivery attempt, should be no message
//      TimeUnit.MILLISECONDS.sleep(messageRedeliveryDelay + 1000)
//      whenReady(LocalMessageQueueMiddleware.receive(queue, 1 second)) { noMessage =>
//        assert(noMessage.isEmpty)
//      }
//
//      Await.result(message.get.complete(), waitForFutureToComplete)
//      // receive after message is completed, should be no message
//      whenReady(LocalMessageQueueMiddleware.receive(queue, 1 second)) { noMessage =>
//        assert(noMessage.isEmpty)
//      }
//
//      val queueName_ExpiredMessage = "QueueExpiredMessage"
//      val queue_expiredMessage: Try[Queue] = LocalMessageQueueMiddleware.createQueueIfAbsent(queueName_ExpiredMessage)
//      // message should expire
//      val testContents2 = "Test message_MessageShouldExpire"
//      Await.result(LocalMessageQueueMiddleware.send(testContents2, queue_expiredMessage.get), waitForFutureToComplete)
//
//      // first time receive
//      whenReady(LocalMessageQueueMiddleware.receive(queue_expiredMessage.get, 1 second)) { messageExpire =>
//        assert(messageExpire.isDefined)
//        assert(messageExpire.get.contents == testContents2)
//      }
//
//      TimeUnit.MILLISECONDS.sleep(messageTimeToLive + 1000)
//      whenReady(LocalMessageQueueMiddleware.receive(queue_expiredMessage.get, 1 second)) { shouldBeNoMessageExpired =>
//        assert(shouldBeNoMessageExpired.isEmpty)
//      }
//
//      val deleteTry = LocalMessageQueueMiddleware.deleteQueue(queueName)
//      assert(deleteTry.isSuccess)
//      val deleteExpiredMessageQueueTry = LocalMessageQueueMiddleware.deleteQueue(queueName_ExpiredMessage)
//      assert(deleteExpiredMessageQueueTry.isSuccess)
//      assert(LocalMessageQueueMiddleware.queues.get.isEmpty)
//    }
//  }
//
//  "BlockingQueue" should "be able to send and receive a few messages" in {
//
//    val queues: Seq[Queue] = LocalMessageQueueMiddleware.queues.get
//    queues.foreach({queue: Queue => LocalMessageQueueMiddleware.deleteQueue(queue.name)})
//
//    val queueName = "receiveAFewMessages"
//
//    assert(LocalMessageQueueMiddleware.queues.get.isEmpty)
//
//    val queue = LocalMessageQueueMiddleware.createQueueIfAbsent(queueName).get
//
//    assert(LocalMessageQueueMiddleware.queues.get == Seq(queue))
//
//    val testContents1 = "First test message_receiveAFewMessages"
//    Await.result(LocalMessageQueueMiddleware.send(testContents1, queue), waitForFutureToComplete)
//    val message: Option[Message] = whenReady(LocalMessageQueueMiddleware.receive(queue, 1 second)) { message =>
//      assert(message.isDefined)
//      assert(message.get.contents == testContents1)
//      message
//    }
//
//    Await.result(message.get.complete(), waitForFutureToComplete)
//
//    whenReady(LocalMessageQueueMiddleware.receive(queue, 1 second)) { shouldBeNoMessage =>
//      assert(shouldBeNoMessage.isEmpty)
//    }
//
//
//    val testContents2 = "Second test message_receiveAFewMessages"
//    Await.result(LocalMessageQueueMiddleware.send(testContents2, queue), waitForFutureToComplete)
//
//    val testContents3 = "Third test message_receiveAFewMessages"
//    Await.result(LocalMessageQueueMiddleware.send(testContents3, queue), waitForFutureToComplete)
//
//    val message2: Option[Message] = whenReady(LocalMessageQueueMiddleware.receive(queue, 1 second)) {message2 =>
//      assert(message2.isDefined)
//      assert(message2.get.contents == testContents2)
//      message2
//    }
//
//    Await.result(message2.get.complete(), waitForFutureToComplete)
//
//    val message3: Option[Message] = whenReady(LocalMessageQueueMiddleware.receive(queue, 1 second)) { message3 =>
//      assert(message3.isDefined)
//      assert(message3.get.contents == testContents3)
//      message3
//    }
//
//    Await.result(message3.get.complete(), waitForFutureToComplete)
//
//    whenReady(LocalMessageQueueMiddleware.receive(queue, 1 second)) { shouldBeNoMessage4 =>
//      assert(shouldBeNoMessage4.isEmpty)
//    }
//
//    val deleteTry = LocalMessageQueueMiddleware.deleteQueue(queueName)
//    assert(deleteTry.isSuccess)
//    assert(LocalMessageQueueMiddleware.queues.get.isEmpty)
//  }
//
//  "BlockingQueue" should "be OK if asked to create the same queue twice " in {
//
//    val queues: Seq[Queue] = LocalMessageQueueMiddleware.queues.get
//    queues.foreach({queue: Queue => LocalMessageQueueMiddleware.deleteQueue(queue.name)})
//
//    val queueName = "createSameQueueTwice"
//    val queue = LocalMessageQueueMiddleware.createQueueIfAbsent(queueName)
//    assert(queue.isSuccess)
//    val sameQueue = LocalMessageQueueMiddleware.createQueueIfAbsent(queueName)
//    assert(sameQueue.isSuccess)
//
//    assert(LocalMessageQueueMiddleware.queues.get == Seq(sameQueue.get))
//    val deleteTry = LocalMessageQueueMiddleware.deleteQueue(queueName)
//    assert(deleteTry.isSuccess)
//    assert(LocalMessageQueueMiddleware.queues.get.isEmpty)
//  }
//
//  "BlockingQueue" should "return a failure if deleting a non-existing queue" in {
//
//    val queues: Seq[Queue] = LocalMessageQueueMiddleware.queues.get
//    queues.foreach({queue: Queue => LocalMessageQueueMiddleware.deleteQueue(queue.name)})
//
//    val queueName = "DeletingNonExistingQueue"
//    val deleteQueue = LocalMessageQueueMiddleware.deleteQueue(queueName)
//    assert(deleteQueue.isFailure)
//    assert(LocalMessageQueueMiddleware.queues.get.isEmpty)
//  }
//
//  "BlockingQueue" should "return a failure if sending message to a non-existing queue" in {
//
//    val queues: Seq[Queue] = LocalMessageQueueMiddleware.queues.get
//    queues.foreach({queue: Queue => LocalMessageQueueMiddleware.deleteQueue(queue.name)})
//
//    val queueName = "SendToNonExistingQueue"
//    an [QueueDoesNotExistException] should be thrownBy Await.result(LocalMessageQueueMiddleware.send("testContent", Queue(queueName)), waitForFutureToComplete)
//
//    assert(LocalMessageQueueMiddleware.queues.get.isEmpty)
//  }
//
//  "BlockingQueue" should "return a failure if receiving a message to a non-existing queue" in {
//
//    val queues: Seq[Queue] = LocalMessageQueueMiddleware.queues.get
//    queues.foreach({queue: Queue => LocalMessageQueueMiddleware.deleteQueue(queue.name)})
//
//    val queueName = "ReceiveFromNonExistingQueue"
//    a [QueueDoesNotExistException] should be thrownBy Await.result(LocalMessageQueueMiddleware.receive(Queue(queueName), Duration(1, "second")), waitForFutureToComplete)
//
//    assert(LocalMessageQueueMiddleware.queues.get.isEmpty)
//  }
//
//  "BlockingQueue" should "be able to filter the special characters in queue name" in {
//    val queues: Seq[Queue] = LocalMessageQueueMiddleware.queues.get
//    queues.foreach({queue: Queue => LocalMessageQueueMiddleware.deleteQueue(queue.name)})
//
//    val queueName = "test# Qu%eueFilter"
//
//    assert(Queue(queueName).name == "testQueueFilter")
//  }
//
//  "BlockingQueue" should "make sure if two InternalMessages are equal, then they have the same hashCode" in {
//
//    val queues: Seq[Queue] = LocalMessageQueueMiddleware.queues.get
//    queues.foreach({queue: Queue => LocalMessageQueueMiddleware.deleteQueue(queue.name)})
//
//    val id: UUID = UUID.randomUUID()
//    val createdTime = System.currentTimeMillis()
//    val message1: InternalMessage = InternalMessage(id, "message1", createdTime, Queue("to"), 0)
//    val message2: InternalMessage = InternalMessage(id, "message1", createdTime, Queue("to"), 1)
//    assert(message1 == message2)
//    val message3: InternalMessage = InternalMessage(UUID.randomUUID(), "message3", createdTime, Queue("to"), 0)
//    assert(message1 != message3)
//  }
//}