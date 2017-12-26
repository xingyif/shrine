package net.shrine.messagequeuesqs

import net.shrine.messagequeueservice.{Message, Queue}
import net.shrine.messagequeuesqs.SQSMessageQueueMiddleware.SQSMessage
import org.junit.runner.RunWith
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.junit.JUnitRunner
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}
import software.amazon.awssdk.services.sqs.model.{BatchEntryIdsNotDistinctException, EmptyBatchRequestException, OverLimitException, QueueDoesNotExistException}

import scala.collection.immutable.Seq
import scala.concurrent.{Await, Future}
import scala.util.Try
import scala.concurrent.duration._

/**
  * Test create, delete queue, send, and receive message, getQueueNames, and completeMessage using SQS service
  * Created by yifan on 12/19/17.
  */

@RunWith(classOf[JUnitRunner])
class SQSMessageQueueMiddlewareTest extends FlatSpec with BeforeAndAfterAll with ScalaFutures with Matchers {

  val waitForFutureToComplete = Duration("1 minute")

  "SQSMessageQueue" should "create queue, send and receive message, complete message and delete queue" in {

    val queues: Try[Seq[Queue]] = SQSMessageQueueMiddleware.queues
    assert(queues.get.isEmpty)

    val queueName: String = "sqsQueue"
    val sqsQueueTry: Try[Queue] = SQSMessageQueueMiddleware.createQueueIfAbsent(queueName)
    val sqsQueue: Queue = sqsQueueTry.get
    assert(sqsQueueTry.isSuccess)
    assert(sqsQueue.name == queueName)

    val createSameQueue: Try[Queue] = SQSMessageQueueMiddleware.createQueueIfAbsent(queueName)
    assert(createSameQueue.isSuccess)
    assert(createSameQueue.get.name == queueName)

    // get all queues
    val oneQueue: Try[Seq[Queue]] = SQSMessageQueueMiddleware.queues
    assert(oneQueue.isSuccess)
    assert(oneQueue.get == Seq(sqsQueue))

    // get queues with specific prefix
    val queueWithSQSPrefix: Try[Seq[Queue]] = SQSMessageQueueMiddleware.queuesWithPrefix("sqs")
    assert(queueWithSQSPrefix.isSuccess)
    assert(queueWithSQSPrefix.get == Seq(sqsQueue))

    val queueWithPrefix: Try[Seq[Queue]] = SQSMessageQueueMiddleware.queuesWithPrefix("")
    assert(queueWithPrefix.isSuccess)
    assert(queueWithPrefix.get == Seq(sqsQueue))

    val noQueue: Try[Seq[Queue]] = SQSMessageQueueMiddleware.queuesWithPrefix("non")
    assert(noQueue.isSuccess)
    assert(noQueue.get.isEmpty)

    // send one message
    val messageContent: String = "first sqs message"
    Await.result(SQSMessageQueueMiddleware.send(messageContent, sqsQueue), waitForFutureToComplete)

    // receive one message
    val sqsMessage: Message = whenReady(SQSMessageQueueMiddleware.receive(sqsQueue, 1 second)) { messageReceived: Option[Message] =>
      assert(messageReceived.isDefined)
      assert(messageReceived.get.contents == messageContent)
      messageReceived.get
    }

    Await.result(sqsMessage.complete(), waitForFutureToComplete)
    // receive after message is completed, should be no message
    whenReady(SQSMessageQueueMiddleware.receive(sqsQueue, 1 second)) { noMessage =>
      assert(noMessage.isEmpty)
    }

    // send multiple messages at once
    val message0ID: String = "0"
    val message0Content: String = "batch msg 0"
    val message1ID: String = "1"
    val message1Content: String = "batch msg 1"
    val mapOfMessages: Map[String, String] = Map( message0ID -> message0Content, message1ID -> message1Content)
    Await.result(SQSMessageQueueMiddleware.sendMultipleMessages(mapOfMessages, sqsQueue, 2 second), waitForFutureToComplete)

    // receive multiple messages at once
    val receivedSQSMessages: List[Message] = whenReady(SQSMessageQueueMiddleware.receiveMultipleMessages(sqsQueue, 2 second, 10)) { messagesReceived: List[Message] =>
      assert(messagesReceived.nonEmpty)
      assert(messagesReceived.size == 2)
      assert(messagesReceived.head.contents == message0Content)
      assert(messagesReceived.last.contents == message1Content)
      messagesReceived
    }

    receivedSQSMessages.foreach(m => Await.result(m.complete(), waitForFutureToComplete))
    // receive after all messages are completed, should be no message
    whenReady(SQSMessageQueueMiddleware.receive(sqsQueue, 1 second)) { noMessage =>
      assert(noMessage.isEmpty)
    }

    val deleteQueueTry: Try[Unit] = SQSMessageQueueMiddleware.deleteQueue(queueName)
    assert(deleteQueueTry.isSuccess)

  }

  "SQSMessageQueue" should "throw an exception if deleting a non-existing queue" in {

    val queues: Seq[Queue] = SQSMessageQueueMiddleware.queues.get
    queues.foreach({queue: Queue => SQSMessageQueueMiddleware.deleteQueue(queue.name)})

    val queueName = "GetNonExistingQueueUrl"
    val deleteQueue = SQSMessageQueueMiddleware.deleteQueue(queueName)
    assert(deleteQueue.isFailure)
    intercept[QueueDoesNotExistException] {
      SQSMessageQueueMiddleware.deleteQueue(queueName)
    }

    assert(SQSMessageQueueMiddleware.queues.get.isEmpty)
  }

  "SQSMessageQueue" should "throw an exception if sending or receiving message to a non-existing queue" in {
    val queues: Seq[Queue] = SQSMessageQueueMiddleware.queues.get
    queues.foreach({queue: Queue => SQSMessageQueueMiddleware.deleteQueue(queue.name)})

    val queueName = "GetNonExistingQueueUrl"
    an [QueueDoesNotExistException] should be thrownBy Await.result(SQSMessageQueueMiddleware.send("testContent", Queue(queueName)), waitForFutureToComplete)
    an [QueueDoesNotExistException] should be thrownBy Await.result(SQSMessageQueueMiddleware.receive(Queue(queueName), 2 second), waitForFutureToComplete)

    assert(SQSMessageQueueMiddleware.queues.get.isEmpty)
  }

  "SQSMessageQueue" should "receive no message or throw an exception if sending empty message to an existing queue" in {
    val queues: Seq[Queue] = SQSMessageQueueMiddleware.queues.get
    queues.foreach({queue: Queue => SQSMessageQueueMiddleware.deleteQueue(queue.name)})

    val queueName = "queueSendEmptyMessageBatch"
    val createQueue = SQSMessageQueueMiddleware.createQueueIfAbsent(queueName)
    assert(createQueue.isSuccess)
    val messageOpt: Option[Message] = Await.result(SQSMessageQueueMiddleware.receive(createQueue.get, 2 second), waitForFutureToComplete)
    assert(messageOpt.isEmpty)

    an [EmptyBatchRequestException] should be thrownBy Await.result(SQSMessageQueueMiddleware.sendMultipleMessages(Map.empty, createQueue.get), waitForFutureToComplete)

    val map: Map[String, String] = Map("id" -> "content1", "id" -> "content2")
    an [BatchEntryIdsNotDistinctException] should be thrownBy Await.result(SQSMessageQueueMiddleware.sendMultipleMessages(map, createQueue.get), waitForFutureToComplete)

    assert(SQSMessageQueueMiddleware.queues.get.isEmpty)
  }

  "SQSMessageQueue" should "receive no message from the queue" in {
    val queues: Seq[Queue] = SQSMessageQueueMiddleware.queues.get
    queues.foreach({queue: Queue => SQSMessageQueueMiddleware.deleteQueue(queue.name)})

    val queueName = "GetNonExistingQueueUrl"
    an [QueueDoesNotExistException] should be thrownBy Await.result(SQSMessageQueueMiddleware.send("testContent", Queue(queueName)), waitForFutureToComplete)

    assert(SQSMessageQueueMiddleware.queues.get.isEmpty)
  }

  "SQSMessageQueue" should "throw exception when maxNumberOfMessage is exceeds limit" in {
    val queues: Seq[Queue] = SQSMessageQueueMiddleware.queues.get
    queues.foreach({queue: Queue => SQSMessageQueueMiddleware.deleteQueue(queue.name)})

    val queueName = "receiveMultipleMessages"
    an [QueueDoesNotExistException] should be thrownBy Await.result(SQSMessageQueueMiddleware.receiveMultipleMessages(Queue(queueName), 1 second, 1), waitForFutureToComplete)

    val createQueue = SQSMessageQueueMiddleware.createQueueIfAbsent(queueName)
    assert(createQueue.isSuccess)

    val listOfMessages = Await.result(SQSMessageQueueMiddleware.receiveMultipleMessages(createQueue.get, 1 second), waitForFutureToComplete)
    assert(listOfMessages.isEmpty)

    an [OverLimitException] should be thrownBy Await.result(SQSMessageQueueMiddleware.receiveMultipleMessages(createQueue.get, 1 second, 11), waitForFutureToComplete)

    assert(SQSMessageQueueMiddleware.queues.get.isEmpty)
  }

  }