package net.shrine.mom
import net.shrine.spray.DefaultJsonSupport
import org.hornetq.api.core.client.ClientMessage

import scala.collection.immutable.Seq
import scala.concurrent.duration.Duration
/**
  * This object mostly imitates AWS SQS' API via an embedded HornetQ. See http://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/examples-sqs.html
  *
  * @author david
  * @since 7/18/17
  */
//todo a better name
//todo split into a trait, this LocalHornetQ, and RemoteHornetQ versions. The server side of RemoteHornetQ will call this local version.
//todo in 1.23 all but the server side will use the client RemoteHornetQ implementation (which will call to the server at the hub)
//todo in 1.24, create an AwsSqs implementation of the trait

trait HornetQMom {
//  private[mom] def stop()
//  private def withSession[T](block: ClientSession => T):T // TODO: prob don't need this
  def createQueueIfAbsent(queueName:String):Queue
  def deleteQueue(queueName:String)
  def queues:Seq[Queue]
  def send(contents:String,to:Queue):Unit
  def receive(from:Queue,timeout:Duration):Option[Message]
  def completeMessage(message:Message):Unit

}
case class Queue(name:String) extends DefaultJsonSupport


case class Message(hornetQMessage:ClientMessage) extends DefaultJsonSupport { //  extends Json4sSupport {
  //    override implicit def json4sFormats: Formats = DefaultFormats
  val propName = "contents"

  def contents = hornetQMessage.getStringProperty(propName)

  def complete() = hornetQMessage.acknowledge()
}
object Queue extends DefaultJsonSupport {
  def apply(name: String): Queue = new Queue(name)
}

object Message {
  def apply(name: String): Message = {
    val clientMessage: ClientMessage = name.asInstanceOf[ClientMessage]
    val currentMessage: Message = new Message(clientMessage)
    currentMessage
  }
}