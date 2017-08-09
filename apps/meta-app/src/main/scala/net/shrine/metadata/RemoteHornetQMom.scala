import akka.actor.ActorRefFactory
import net.shrine.log.Loggable
import spray.routing.HttpService

/**
  * Created by yifan on 7/24/17.
  */

class RemoteHornetQMom extends HornetQMom
  with HttpService
  with Loggable {

  // creates a new Queue (sqs: standard/FIFO)
  def createQueue(createQueueRequest: CreateQueueRequest): CreateQueueResult = ???
  // Simplified method form for invoking the CreateQueue operation.
  def createQueue(queueName: String): CreateQueueResult = ???

  class CreateQueueRequest(private var queueName: String) {
    // todo search scala nested class for syntax
    def addAttributesEntry(key: String, value: String): CreateQueueRequest = ???
    // Removes all the entries added into Attributes.
    def clearAttributesEntries(): CreateQueueRequest = ??? // todo shouldn't this be Unit
    // A map of attributes with their corresponding values.
    def getAttributes(): Map<String, String> = ???

    // A map of attributes with their corresponding values.
    def setAttributes(attributes: Map<String, String>): Unit = ???


    // The name of the new queue.
    def getQueueName(): String = {this.queueName} // todo this syntax
    // Changes the name of the queue
    def setQueueName(queueName: String): Unit = {this.queueName = queueName}
  }

  // Deletes the queue specified by the QueueUrl, even if the queue is empty.
  def deleteQueue(deleteQueueRequest: DeleteQueueRequest): DeleteQueueResult = ???
  //   Simplified method form for invoking the DeleteQueue operation.
  def deleteQueue(queueUrl: String): DeleteQueueResult = ???




  override def createQueueIfAbsent(queueName: String) = ???
  lazy val

  override def deleteQueue(queueName: String): Unit = ???

  override def queues: Seq[Any] = ???

  override def send(contents: String, to: Any): Unit = ???

  override def receive(from: Any, timeout: Any): Option[Any] = ???

  override def complete(message: Any): Unit = ???

  override implicit def actorRefFactory: ActorRefFactory = ???
}