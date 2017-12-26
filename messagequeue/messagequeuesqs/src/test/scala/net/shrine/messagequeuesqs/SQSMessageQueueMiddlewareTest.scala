package net.shrine.messagequeuesqs

import net.shrine.messagequeueservice.{Message, Queue}
import net.shrine.messagequeuesqs.SQSMessageQueueMiddleware.SQSMessage
import org.junit.runner.RunWith
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.junit.JUnitRunner
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}

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

    // get all queues
    val oneQueue: Try[Seq[Queue]] = SQSMessageQueueMiddleware.queues
    assert(oneQueue.isSuccess)
    assert(oneQueue.get == Seq(sqsQueue))

    // get queues with specific prefix
    val queueWithPrefix: Try[Seq[Queue]] = SQSMessageQueueMiddleware.queuesWithPrefix("sqs")
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
}