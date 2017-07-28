package net.shrine.metadata

import akka.actor.{Actor, ActorRefFactory}
import akka.event.Logging
import net.shrine.log.Loggable
import net.shrine.mom.{HornetQMom, LocalHornetQMom, Message, Queue}
import spray.http.{HttpRequest, HttpResponse, StatusCode, StatusCodes}
import spray.routing.directives.LogEntry
import spray.routing.{HttpService, Route}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration
import scala.util.Try
/**
  * Created by yifan on 7/24/17.
  */

trait RemoteHornetQMom extends HornetQMom  // todo RemoteHornetQMom needs to be a trait to be mix in
  with HttpService
  with Loggable {
  //  private implicit def ec = actorRefFactory.dispatcher
  //  lazy val staticDataRoute: Route = get {
  //    (pathPrefix("staticData") | pathPrefix("data")) {
  //      parameter("key") { (key: String) =>
  //        complete(handleKey(key))
  //      } ~ complete(handleAll) ~
  //        pathEnd ( complete(staticInfo) )
  //    }}
  //
  //  def handleAll:(StatusCode, String) = {
  //    StatusCodes.OK -> staticDataConfig.root.render(ConfigRenderOptions.concise()) // returns it as JSON.
  //  }
  //
  //  def handleKey(key: String): (StatusCode, String) = {
  //    Try(StatusCodes.OK -> staticDataConfig.getValue(key).render(ConfigRenderOptions.concise()))
  //      .getOrElse(StatusCodes.NotFound ->
  //        s"Could not find a value for the specified path `$key`")
  //  }
  lazy val createQueue: Route = get {
    pathPrefix("createQueue") {
      parameter('queueName) { (queueName: String) =>
        complete(createQueueIfAbsent(queueName))
      }
    }
  }

  // todo SQS returns CreateQueueResult, which contains queueUrl: String
  override def createQueueIfAbsent(queueName: String): (StatusCode, Queue) = {

    val response: Queue = LocalHornetQMom.createQueueIfAbsent(queueName)
    response.json4sMarshaller // return as JSON
    // throw alreadyExists SQS QueueAlreadyExists
    StatusCodes.OK -> response
    //          .getOrElse(StatusCodes.NotFound ->
    //            s"Could not find a value for the specified path ``")
  }

  lazy val deleteQueue: Route = pathPrefix("deleteQueue") {
    parameter('queueName) { (queueName: String) =>
      deleteQueue(queueName)
      complete(s"Queue '$queueName' deleted!") // todo
    }
  }

  override def deleteQueue(queueName: String): (StatusCode, Unit) = {
    // todo SQS takes in DeleteMessageRequest, which contains a queueUrl: String and a ReceiptHandle: String
    // returns a DeleteMessageResult, toString for debugging
    Try(StatusCodes.OK -> LocalHornetQMom.deleteQueue(queueName))
      .getOrElse(StatusCodes.BadRequest
        -> s"Given QueueName doesn't exist! Failed to delete!")
  }

  lazy val sendMessage: Route = pathPrefix("sendMessage") {
    parameters('messageContent, 'toQueue) { (messageContent: String, toQueue: String) => {
      send(messageContent, Queue.apply(toQueue)) // todo what is a better way to complete
      complete(s"Message sent to $toQueue!")
    }
    }
  }

  // todo SQS sendMessage(String queueUrl, String messageBody) => SendMessageResult
  override def send(contents: String, to: Queue): (StatusCode, Unit) = {
    Try(StatusCodes.OK -> LocalHornetQMom.send(contents, to))
      .getOrElse(StatusCodes.BadRequest ->
        s"HornetQException occurred sending the message $contents to queue $to !")
  }

  lazy val receiveMessage: Route = pathPrefix("receiveMessage") {
    parameters('fromQueue, 'timeOutDuration) { (fromQueue, timeOutDuration) => {
      val timeout: Duration = Duration.apply(timeOutDuration)
      receive(Queue.apply(fromQueue), timeout)
      complete(s"Message received from $fromQueue!")
    }
    }
  }

  // todo SQS ReceiveMessageResult receiveMessage(String queueUrl)
  override def receive(from: Queue, timeout: Duration): (StatusCode, Option[Message]) = {
    StatusCodes.OK -> LocalHornetQMom.receive(from, timeout)
    //        .getOrElse(StatusCodes.BadRequest ->
    //        s"Not able to get")
  }

  lazy val acknowledge = pathPrefix("acknowledge") {
    parameter('message) { (message: String) =>
      completeMessage(Message.apply(message))
      complete(s"$message acknowledged!")
    }
  }

  // todo SQS has DeleteMessageResult deleteMessage(String queueUrl, String receiptHandle)
  override def completeMessage(message: Message): (StatusCode, Unit) = {
    Try(StatusCodes.OK -> LocalHornetQMom.completeMessage(message))
      .getOrElse(StatusCodes.BadGateway ->
        s"HornetQException occurred while acknowledging the message!")
  }


  // Returns the names of the queues created on this server. Seq[Any]
  lazy val status: Route = pathPrefix("status") {
    complete(queues)
  }

  override def queues: Seq[Queue] = LocalHornetQMom.queues

//  val route =
//    parameters('color, 'backgroundColor) { (color, backgroundColor) =>
//      complete(s"The color is '$color' and the background is '$backgroundColor'")
//    }


  //  // creates a new Queue (sqs: standard/FIFO)
  //  def createQueue(createQueueRequest: CreateQueueRequest): CreateQueueResult = ???
  //  // Simplified method form for invoking the CreateQueue operation.
  //  def createQueue(queueName: String): CreateQueueResult = ???
  //
  //  class CreateQueueRequest(private var queueName: String) {
  //    // todo search scala nested class for syntax
  //    def addAttributesEntry(key: String, value: String): CreateQueueRequest = ???
  //    // Removes all the entries added into Attributes.
  //    def clearAttributesEntries(): CreateQueueRequest = ??? // todo shouldn't this be Unit
  //    // A map of attributes with their corresponding values.
  //    def getAttributes(): Map<String, String> = ???
  //
  //    // A map of attributes with their corresponding values.
  //    def setAttributes(attributes: Map<String, String>): Unit = ???
  //
  //
  //    // The name of the new queue.
  //    def getQueueName(): String = {this.queueName} // todo this syntax
  //    // Changes the name of the queue
  //    def setQueueName(queueName: String): Unit = {this.queueName = queueName}


  // Deletes the queue specified by the QueueUrl, even if the queue is empty.
  //  def deleteQueue(deleteQueueRequest: DeleteQueueRequest): DeleteQueueResult = ???
  //   Simplified method form for invoking the DeleteQueue operation.
  //  def deleteQueue(queueUrl: String): DeleteQueueResult = ???

}