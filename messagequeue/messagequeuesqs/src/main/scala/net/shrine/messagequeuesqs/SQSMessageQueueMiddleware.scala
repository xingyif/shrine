package net.shrine.messagequeuesqs

import java.util

import com.typesafe.config.Config
import net.shrine.messagequeueservice.{Message, MessageQueueService, Queue}
import software.amazon.awssdk.core.auth.ProfileCredentialsProvider
import software.amazon.awssdk.core.exception.{SdkClientException, SdkException, SdkServiceException}
import software.amazon.awssdk.core.regions.Region
import software.amazon.awssdk.services.sqs.model.{CreateQueueRequest, DeleteMessageRequest, DeleteQueueRequest, DeleteQueueResponse, GetQueueUrlRequest, GetQueueUrlResponse, ListQueuesResponse, QueueDeletedRecentlyException, QueueDoesNotExistException, QueueNameExistsException, ReceiveMessageRequest, SQSException, SendMessageRequest}
import software.amazon.awssdk.services.sqs.{SQSClient, model}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.JavaConversions._
import scala.collection.immutable.Seq
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success, Try}
import net.shrine.source.ConfigSource
import net.shrine.config.ConfigExtensions
import net.shrine.log.Log

import scala.concurrent.Future

/**
  * A web API that provides access to AWS SimpleQueueService.
  * Allows client to createQueue, deleteQueue, sendMessage, receiveMessage, getQueues, and sendReceipt
  *
  * Created by yifan on 12/18/17.
  */
object SQSMessageQueueMiddleware extends MessageQueueService {

//  val credentialsProvider: ProfileCredentialsProvider = new ProfileCredentialsProvider
//    try {
//      credentialsProvider.getCredentials
//    }
//    catch {
//      case e: Exception =>
//        throw new Nothing("Cannot load the credentials from the credential profiles file. "
//          + "Please make sure that your credentials file is at the correct "
//          + "location (~/.aws/credentials), and is in valid format.", e)
//    }

  // .credentialsProvider(credentialsProvider)
  val sqsClient: SQSClient = SQSClient.builder.region(Region.US_EAST_1).build

  val configPath = "shrine.messagequeue.blockingq" // todo change the name in config file to "messageq"?

  def config: Config = ConfigSource.config.getConfig(configPath)

  private def messageTimeToLiveInMillis: Long = config.get("messageTimeToLive", Duration(_)).toMillis

  private def messageRedeliveryDelay: Long = config.get("messageRedeliveryDelay", Duration(_)).toMillis

  private def messageMaxDeliveryAttempts: Int = config.getInt("messageMaxDeliveryAttempts")

  /**
    * creates a queue using the given queueName
    * @param queueName
    * @return Result of the CreateQueue operation returned by the service.
    * @throws QueueDeletedRecentlyException
    *         You must wait 60 seconds after deleting a queue before you can create another one with the same name.
      @throws QueueNameExistsException
              A queue already exists with this name. Amazon SQS returns this error only if the request includes attributes whose values differ from those of the existing queue.
      @throws SdkException
              Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for catch all scenarios.
      @throws SdkClientException
              If any client side error occurs such as an IO related failure, failure to get credentials, etc.
      @throws SQSException
              Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
      @throws SdkServiceException
    */
  def createQueueIfAbsent(queueName: String): Try[Queue] = Try {
    val createQueueRequest: CreateQueueRequest = CreateQueueRequest.builder().queueName(queueName).build
    sqsClient.createQueue(createQueueRequest)
    Queue(queueName)
  }

  /**
    * gets a queueUrl of an existing queue using the given queueName
    * @param queueName
    * @return Result of the GetQueueUrl operation returned by the service.
    * @throws QueueDoesNotExistException - The queue referred to doesn't exist.
      @throws SdkException
              Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for catch all scenarios.
      @throws SdkClientException
              If any client side error occurs such as an IO related failure, failure to get credentials, etc.
      @throws SQSException
              Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
      @throws SdkServiceException

    */
  private def getQueueUrl(queueName: String): Try[String] = Try {
    val getQueueUrlRequest: GetQueueUrlRequest = GetQueueUrlRequest.builder().queueName(queueName).build()
    val getQueueUrlResponse: GetQueueUrlResponse = sqsClient.getQueueUrl(getQueueUrlRequest)
    getQueueUrlResponse.queueUrl
  }


  /**
    * deletes a queue using the given queueUrl
    *
    * @param queueName
    * @return Result of the DeleteQueue operation returned by the service.
    * @throws SdkException
              Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for catch all scenarios.
      @throws SdkClientException
              If any client side error occurs such as an IO related failure, failure to get credentials, etc.
      @throws SQSException
              Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
      @throws SdkServiceException
    */
  def deleteQueue(queueName: String): Try[Unit] = {
    getQueueUrl(queueName).transform({ queueUrl: String =>
      val deleteQueueRequest: DeleteQueueRequest = DeleteQueueRequest.builder().queueUrl(queueUrl).build()
      val deleteQueueResponse: DeleteQueueResponse = sqsClient.deleteQueue(deleteQueueRequest)
      Success(deleteQueueResponse)
    }, { throwable: Throwable =>
      Log.debug(s"Cannot find the queueUrl of the given queue $queueName")
      Failure(throwable)
    })
  }

  /**
    * queueUrl format: https://{REGION_ENDPOINT}/queue.|api-domain|/{YOUR_ACCOUNT_NUMBER}/{YOUR_QUEUE_NAME}
    *
    * @return Returns a list of queues. The maximum number of queues that can be returned is 1,000.
    * @throws SdkException
              Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for catch all scenarios.
      @throws SdkClientException
              If any client side error occurs such as an IO related failure, failure to get credentials, etc.
      @throws SQSException
              Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
      @throws SdkServiceException
    */
  def queues: Try[Seq[Queue]] = Try {
    val listQueuesResponse: ListQueuesResponse = sqsClient.listQueues()
    val listOfQueueUrls: util.List[String] = listQueuesResponse.queueUrls()
    val result: Seq[Queue] = Seq.empty

    for (url: String <- listOfQueueUrls) {
      val urlArray: Array[String] = url.split("/")
      val urlArrayLength: Int = urlArray.length
      result :+ Queue(urlArray.apply(urlArrayLength - 1))
    }
    result
  }

  // todo list queue with filters: name prefix

  /**
    * Sends the given message to a given Queue
    * @param contents message contents
    * @param to
    * @return Result of the SendMessage operation returned by the service.
    */
  def send(contents:String,to:Queue): Future[Unit] = Future {
    Future.fromTry(getQueueUrl(to.name)).transform({queueUrl: String =>
      // user can set delay seconds for send
      val sendMessageRequest: SendMessageRequest = SendMessageRequest.builder().queueUrl(queueUrl).messageBody(contents).build()
      sqsClient.sendMessage(sendMessageRequest)
    }, {throwable: Throwable =>
      Log.debug(s"Cannot find the queueUrl of the given queue ${to.name}")
      throwable
    })
  }

  // todo Send multiple messages at once

  /**
    * Receives message from the given queue within the given timeout
    * Can set maxNumberOfMessages by ReceiveMessageRequest.builder.queueUrl(queueUrl).maxNumberOfMessages(INT)
    * @param from
    * @param timeout
    * @return One Message or None from the given queue
    */
  def receive(from:Queue,timeout:Duration): Future[Option[Message]] = {
    Future.fromTry(getQueueUrl(from.name)).transform({ queueUrl: String =>
      val receiveMessageRequest: ReceiveMessageRequest = ReceiveMessageRequest.builder.queueUrl(queueUrl).waitTimeSeconds(timeout.toSeconds.toInt).build
      val messages: util.List[model.Message] = sqsClient.receiveMessage(receiveMessageRequest).messages
      if (messages.isEmpty) Log.info(s"No message available from the queue ${from.name} within timeout $timeout")
      messages.headOption.map(m => SQSMessage(m, from))
    }, { throwable: Throwable =>
      Log.debug(s"Cannot find the queueUrl of the given queue ${from.name}")
      throwable
    })
  }

  case class SQSMessage(message: model.Message, belongsToQueue: Queue) extends Message {

    /**
      * AKA DeleteMessage
      * deletes the message from its queue
      * @return Result of the DeleteMessage operation returned by the service.
      */
    override def complete(): Future[Unit] = {
      Future.fromTry(getQueueUrl(belongsToQueue.name)).transform({ queueUrl: String =>
        val deleteMessageRequest = DeleteMessageRequest.builder.queueUrl(queueUrl).receiptHandle(message.receiptHandle()).build
        sqsClient.deleteMessage(deleteMessageRequest)
      }, { throwable: Throwable =>
        Log.debug(s"Cannot find the queueUrl of the given queue ${belongsToQueue.name}")
        throwable
      })
    }

    /**
      *
      * @return The message's body (not URL-encoded)
      */
    override def contents: String = {
      message.body()
    }
  }
}
