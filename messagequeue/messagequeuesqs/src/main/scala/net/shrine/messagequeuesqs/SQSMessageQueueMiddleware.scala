package net.shrine.messagequeuesqs

import java.util

import com.typesafe.config.Config
import net.shrine.config.ConfigExtensions
import net.shrine.log.Log
import net.shrine.messagequeueservice.{Message, MessageQueueService, Queue}
import net.shrine.source.ConfigSource
import software.amazon.awssdk.core.auth.ProfileCredentialsProvider
import software.amazon.awssdk.core.exception.{SdkClientException, SdkException, SdkServiceException}
import software.amazon.awssdk.core.regions.Region
import software.amazon.awssdk.services.sqs.model.{BatchEntryIdsNotDistinctException, BatchRequestTooLongException, CreateQueueRequest, CreateQueueResponse, DeleteMessageRequest, DeleteQueueRequest, DeleteQueueResponse, EmptyBatchRequestException, GetQueueUrlRequest, GetQueueUrlResponse, InvalidBatchEntryIdException, InvalidIdFormatException, InvalidMessageContentsException, ListQueuesRequest, ListQueuesResponse, OverLimitException, QueueDeletedRecentlyException, QueueDoesNotExistException, QueueNameExistsException, ReceiptHandleIsInvalidException, ReceiveMessageRequest, SQSException, SendMessageBatchRequest, SendMessageBatchRequestEntry, SendMessageBatchResponse, SendMessageRequest, TooManyEntriesInBatchRequestException}
import software.amazon.awssdk.services.sqs.{SQSClient, model}

import scala.collection.JavaConversions._
import scala.collection.JavaConverters
import scala.collection.immutable.Seq
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.concurrent.duration._
import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}

/**
  * A web API that provides access to AWS SimpleQueueService.
  * Allows client to createQueue, deleteQueue, sendMessage, receiveMessage, getQueues, and sendReceipt
  *
  * Created by yifan on 12/18/17.
  */
object SQSMessageQueueMiddleware extends MessageQueueService {

  val credentialsProvider: ProfileCredentialsProvider = ProfileCredentialsProvider.create()
    try {
      credentialsProvider.getCredentials
    }
    catch {
      case NonFatal(nf) =>
        throw new IllegalArgumentException("Cannot load the credentials from the credential profiles file. "
          + "Please make sure that your credentials file is at the correct "
          + "location (~/.aws/credentials), and is in valid format.", nf)
    }

  // .credentialsProvider(credentialsProvider)
  val sqsClient: SQSClient = SQSClient.builder.credentialsProvider(credentialsProvider).region(Region.US_EAST_1).build

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
    */
  def createQueueIfAbsent(queueName: String): Try[Queue] = Try {
    val createQueueRequest: CreateQueueRequest = CreateQueueRequest.builder().queueName(queueName).build
    sqsClient.createQueue(createQueueRequest)
  }.transform({response: CreateQueueResponse =>
    Success(Queue(queueName))
  }, { throwable: Throwable =>
    throwable match {
      case qnee: QueueNameExistsException => {
        Log.info(s"Queue $queueName already exists, not creating a queue.")
        Success(Queue(queueName))
      }
      case qdre: QueueDeletedRecentlyException => {
        Log.error(s"ERROR: Failed to create Queue $queueName You must wait 60 seconds after deleting a queue before you can create another one with the same name.")
        throw qdre
      }
      case NonFatal(nf) => {
        Log.error(s"ERROR: exception $nf", nf)
        throw nf
      }
    }
  })

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
    sqsClient.getQueueUrl(getQueueUrlRequest)
  }.transform({getQueueUrlResponse: GetQueueUrlResponse =>
    Success(getQueueUrlResponse.queueUrl)
  }, {throwable: Throwable =>
    throwable match {
      case qdnee: QueueDoesNotExistException => {
        Log.error(s"ERROR: Queue $queueName does not exist!", qdnee)
        throw qdnee
      }
      case NonFatal(nf) => {
        Log.error(s"ERROR: exception $nf", nf)
        throw nf
      }
    }
  })


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
    */
  def deleteQueue(queueName: String): Try[Unit] = {
    getQueueUrl(queueName).transform({ queueUrl: String =>  // deal with exceptions while getting the queueUrl
      val deleteQueueRequest: DeleteQueueRequest = DeleteQueueRequest.builder().queueUrl(queueUrl).build()
      val deleteQueueResponse: DeleteQueueResponse = sqsClient.deleteQueue(deleteQueueRequest)
      println(deleteQueueResponse)
      Success(deleteQueueResponse) // todo interpret the response
    }, { throwable: Throwable =>
      Log.error(s"ERROR: Cannot find the queueUrl of the given queue $queueName due to exception $throwable")
      Failure(throwable)
    })
  }.transform({deleteQueueResponse: DeleteQueueResponse => // deal with exceptions while deleting
    Success(deleteQueueResponse)
  }, {throwable: Throwable =>
    throwable match {
      case NonFatal(nf) => {
        Log.error(s"ERROR: Cannot delete Queue $queueName due to exception $throwable", throwable)
        throw nf
      }
    }
  })

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
    */
  def queues: Try[Seq[Queue]] = Try {
    val listQueuesResponse: ListQueuesResponse = sqsClient.listQueues()
    val listOfQueueUrls: util.List[String] = listQueuesResponse.queueUrls()
    val seqOfQueueUrls: Seq[String] = JavaConverters.collectionAsScalaIterableConverter(listOfQueueUrls).asScala.to[collection.immutable.Seq]
    val result: Seq[Queue] = seqOfQueueUrls.map({ url: String =>
      val urlArray: Array[String] = url.split("/")
      val urlArrayLength: Int = urlArray.length
      Queue(urlArray.apply(urlArrayLength - 1))
    })
    result
  }.transform({seqOfQueues: Seq[Queue] =>
    Success(seqOfQueues)
  }, { throwable: Throwable => // deal with exceptions while getting all the queues
    throwable match {
      case NonFatal(nf) => {
        Log.error(s"ERROR: Cannot get all the queues due to exception $throwable", throwable)
        throw nf
      }
    }
  })

  /**
    *
    * @param prefix the prefix of the queue name
    * @return list of queues with the given name prefix
    *
    * @throws SdkException
              Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for catch all scenarios.
      @throws SdkClientException
              If any client side error occurs such as an IO related failure, failure to get credentials, etc.
      @throws SQSException
              Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
      @throws SdkServiceException
    */
  def queuesWithPrefix(prefix: String): Try[Seq[Queue]] = Try {
    val listQueuesRequest = ListQueuesRequest.builder.queueNamePrefix(prefix).build
    val listQueuesResponse: ListQueuesResponse = sqsClient.listQueues(listQueuesRequest)
    val listOfQueueUrls: util.List[String] = listQueuesResponse.queueUrls()
    val seqOfQueueUrls: Seq[String] = JavaConverters.collectionAsScalaIterableConverter(listOfQueueUrls).asScala.to[collection.immutable.Seq]
    val result: Seq[Queue] = seqOfQueueUrls.map({ url: String =>
      val urlArray: Array[String] = url.split("/")
      val urlArrayLength: Int = urlArray.length
      Queue(urlArray.apply(urlArrayLength - 1))
    })
    result
  }.transform({seqOfQueues: Seq[Queue] =>
    Success(seqOfQueues)
  }, { throwable: Throwable => // deal with exceptions while getting all the queues with the given specific prefix
    throwable match {
      case NonFatal(nf) => {
        Log.error(s"ERROR: Cannot get the queues with prefix $prefix due to exception $throwable", throwable)
        throw nf
      }
    }
  })

  /**
    * Sends the given message to a given Queue
    *
    * @param contents message contents
    * @param to which queue to send to
    * @return Result of the SendMessage operation returned by the service.'
    * @throws InvalidMessageContentsException
    *         The message contains characters outside the allowed set.
    * @throws UnsupportedOperationException
    *         Error code 400. Unsupported operation.
    * @throws SdkException
    *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
    *         catch all scenarios.
    * @throws SdkClientException
    *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
    * @throws SQSException
    *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
    */
  def send(contents:String, to:Queue): Future[Unit] = {
    Future.fromTry(getQueueUrl(to.name)).transform({queueUrl: String =>  // deal with exceptions while getting the queueUrl
      // user can set delay seconds for send
      val sendMessageRequest: SendMessageRequest = SendMessageRequest.builder().queueUrl(queueUrl).messageBody(contents).build()
      val sendResponse = sqsClient.sendMessage(sendMessageRequest)
      println(sendResponse)
    }, {throwable: Throwable =>
      Log.error(s"ERROR: Cannot find the queueUrl of the given queue ${to.name} due to exception $throwable", throwable)
      throwable
    })
  }.transform({ s: Unit =>
    s
  }, { throwable: Throwable => // deal with exceptions while sending the message
    throwable match {
      case imce: InvalidMessageContentsException => {
        Log.error(s"ERROR: Cannot send message to Queue $to, because the message contains characters outside the allowed set. Message content $contents", imce)
        throw imce
      }
      case uoe: UnsupportedOperationException => {
        Log.error(s"ERROR: Cannot send message to Queue $to, because the operation is not supported by SQS", uoe)
        throw uoe
      }
      case NonFatal(nf) => {
        Log.error(s"ERROR: Cannot get all the queues due to exception $throwable", throwable)
        throw nf
      }
    }
  })

  /**
    * Send multiple messages at once
    *
    * @param messagesMap hashMap of entries of [messageId, messageContent]
    * @param to which queue to send to
    * @param delaySecond seconds to wait before sending the messages
    * @return
    * *
    * @throws TooManyEntriesInBatchRequestException
    *         The batch request contains more entries than permissible.
    * @throws EmptyBatchRequestException
    *         The batch request doesn't contain any entries.
    * @throws BatchEntryIdsNotDistinctException
    *         Two or more batch entries in the request have the same <code>Id</code>.
    * @throws BatchRequestTooLongException
    *         The length of all the messages put together is more than the limit.
    * @throws InvalidBatchEntryIdException
    *         The id of a batch entry in a batch request doesn't abide by the specification.
    * @throws UnsupportedOperationException
    *         Error code 400. Unsupported operation.
    * @throws SdkException
    *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
    *         catch all scenarios.
    * @throws SdkClientException
    *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
    * @throws SQSException
    *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type
    */
  def sendMultipleMessages(messagesMap: Map[String, String], to: Queue, delaySecond: Duration = 0 second): Future[Unit] = {
    Future.fromTry(getQueueUrl(to.name)).transform({ queueUrl: String => // deal with exceptions thrown while getting the queueUrl
      val listOfSendMessageBatchRequestEntry: List[SendMessageBatchRequestEntry] = messagesMap.map{ idAndContent: (String, String) =>
        SendMessageBatchRequestEntry.builder
          .id(idAndContent._1)
          .messageBody(idAndContent._2)
          .delaySeconds(delaySecond.toSeconds.toInt).build()
      }.toList
      val sendMessageBatchRequest: SendMessageBatchRequest = SendMessageBatchRequest.builder
          .queueUrl(queueUrl).entries(listOfSendMessageBatchRequestEntry).build()
        sqsClient.sendMessageBatch(sendMessageBatchRequest)
    }, {throwable: Throwable =>
      Log.error(s"ERROR: Cannot find the queueUrl of the given queue ${to.name} due to exception $throwable", throwable)
      throwable
    })
  }.transform({ s: SendMessageBatchResponse =>
    s
  }, { throwable: Throwable => // deal with exceptions while sending multiple messages
    throwable match {
      case tmeibre: TooManyEntriesInBatchRequestException => {
        Log.error(s"ERROR: Cannot send multiple messages to Queue $to, because the batch request contains more entries than permissible", tmeibre)
        throw tmeibre
      }
      case ebre: EmptyBatchRequestException => {
        Log.error(s"ERROR: Cannot send multiple messages to Queue $to, because the batch was empty", ebre)
        throw ebre
      }
      case imce: InvalidMessageContentsException => {
        Log.error(s"ERROR: Cannot send message to Queue $to, because the message contains characters outside the allowed set.", imce)
        throw imce
      }
      case beinde: BatchEntryIdsNotDistinctException => {
        Log.error(s"ERROR: Cannot send multiple messages to Queue $to, because two or more batch entries in the request have the same", beinde)
        throw beinde
      }
      case brtle: BatchRequestTooLongException => {
        Log.error(s"ERROR: Cannot send multiple messages to Queue $to, because the length of all the messages put together is more than the limit.", brtle)
        throw brtle
      }
      case ibeie: InvalidBatchEntryIdException => {
        Log.error(s"ERROR: Cannot send multiple messages to Queue $to, because the id of a batch entry in a batch request doesn't abide by the specification", ibeie)
        throw ibeie
      }
      case uoe: UnsupportedOperationException => {
        Log.error(s"ERROR: Cannot send message to Queue $to, because the operation is not supported by SQS", uoe)
        throw uoe
      }
      case NonFatal(nf) => {
        Log.error(s"ERROR: Cannot get all the queues due to exception $throwable", throwable)
        throw nf
      }
    }
  })


  /**
    * Receives message from the given queue within the given timeout
    *
    * @param from
    * @param timeout
    * @return One Message or None from the given queue
    * @throws SdkException
    * Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
    * catch all scenarios.
    * @throws SdkClientException
    * If any client side error occurs such as an IO related failure, failure to get credentials, etc.
    * @throws SQSException
    * Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
    */
  def receive(from:Queue,timeout:Duration): Future[Option[Message]] = {
    Future.fromTry(getQueueUrl(from.name)).transform({ queueUrl: String =>
      val receiveMessageRequest: ReceiveMessageRequest = ReceiveMessageRequest.builder.queueUrl(queueUrl).waitTimeSeconds(timeout.toSeconds.toInt).build
      val messages: util.List[model.Message] = sqsClient.receiveMessage(receiveMessageRequest).messages
      if (messages.isEmpty) Log.info(s"No message available from the queue ${from.name} within timeout $timeout")
      val m = messages.headOption.map(m => SQSMessage(m, from))
      println(m)
      m
    }, { throwable: Throwable =>
      Log.error(s"ERROR: Cannot find the queueUrl of the given queue ${from.name} due to exception $throwable")
      throwable
    })
  }.transform({message: Option[Message] =>
    message
  }, {throwable: Throwable =>
    throwable match {
      case NonFatal(nf) => {
        Log.error(s"ERROR: Cannot receive message from Queue $from within Timeout $timeout due to $throwable", nf)
        throw nf
      }
    }
  })

  /**
    * Receives messages from the given queue within the given timeout
    * maxNumberOfMessages default value is 1, valid value is from 1 to 10, less number of messages can be returned
    *
    * @param from
    * @param timeout
    * @return Message(s) or None from the given queue
    * @throws OverLimitException
    * The action that you requested would violate a limit. If the maximum number of messages is reached
    * @throws SdkException
    * Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
    * catch all scenarios.
    * @throws SdkClientException
    * If any client side error occurs such as an IO related failure, failure to get credentials, etc.
    * @throws SQSException
    * Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
    */
  def receiveMultipleMessages(from:Queue, timeout:Duration, maxNumberOfMessages: Int = 1): Future[List[Message]] = {
    Future.fromTry(getQueueUrl(from.name)).transform({ queueUrl: String =>
      val receiveMessageRequest: ReceiveMessageRequest = ReceiveMessageRequest.builder
        .queueUrl(queueUrl)
        .maxNumberOfMessages(maxNumberOfMessages)
        .waitTimeSeconds(timeout.toSeconds.toInt)
        .build
      val messages: util.List[model.Message] = sqsClient.receiveMessage(receiveMessageRequest).messages
      if (messages.isEmpty) Log.info(s"No message available from the queue ${from.name} within timeout $timeout")
      messages.toList.map(m => SQSMessage(m, from))
    }, { throwable: Throwable =>
      Log.error(s"ERROR: Cannot find the queueUrl of the given queue ${from.name}", throwable)
      throwable
    })
  }.transform({messages: List[Message] =>
    messages
  }, {throwable: Throwable =>
    throwable match {
      case ole: OverLimitException => {
        Log.error(s"ERROR: Cannot receive multiple messages from Queue $from within Timeout $timeout," +
          s" because the given maxNumberOfMessages is invalid. Valid value is from 1 to 10.", ole)
        throw ole
      }
      case NonFatal(nf) => {
        Log.error(s"ERROR: Cannot receive multiple messages from Queue $from within Timeout $timeout due to $throwable", nf)
        throw nf
      }
    }
  })

  case class SQSMessage(message: model.Message, belongsToQueue: Queue) extends Message {

    /**
      * AKA DeleteMessage in AWS SQS
      * deletes the message from its queue
      *
      * @return Result of the DeleteMessage operation returned by the service.
      * @throws InvalidIdFormatException
      * The receipt handle isn't valid for the current version.
      * @throws ReceiptHandleIsInvalidException
      * The receipt handle provided isn't valid.
      * @throws SdkException
      * Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
      * catch all scenarios.
      * @throws SdkClientException
      * If any client side error occurs such as an IO related failure, failure to get credentials, etc.
      * @throws SQSException
      * Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
      */
    override def complete(): Future[Unit] = {
      Future.fromTry(getQueueUrl(belongsToQueue.name)).transform({ queueUrl: String =>
        val deleteMessageRequest = DeleteMessageRequest.builder.queueUrl(queueUrl).receiptHandle(message.receiptHandle()).build
        val response = sqsClient.deleteMessage(deleteMessageRequest)
        println(response)
      }, { throwable: Throwable =>
        Log.error(s"ERROR: Cannot find the queueUrl of the given queue ${belongsToQueue.name} due to $throwable", throwable)
        throwable
      })
    }.transform({s: Unit =>
      s
    }, { throwable: Throwable =>
      throwable match {
        case iife: InvalidIdFormatException => {
          Log.error(s"ERROR: Cannot complete message that belongs to Queue ${belongsToQueue.name} because the receipt handle isn't valid for the current version", iife)
          throw iife
        }
        case rhiie: ReceiptHandleIsInvalidException => {
          Log.error(s"ERROR: Cannot complete message that belongs to Queue ${belongsToQueue.name} because the receipt handle provided isn't valid", rhiie)
          throw rhiie
        }
        case NonFatal(nf) => {
          Log.error(s"ERROR: Cannot complete message that belongs to Queue ${belongsToQueue.name} due to $nf", nf)
          throw nf
        }
      }
    })

    /**
      *
      * @return The message's body (not URL-encoded)
      */
    override def contents: String = {
      message.body()
    }
  }
}
