package net.shrine.mom
import net.shrine.spray.DefaultJsonSupport
import org.hornetq.api.core.client.ClientMessage
import org.hornetq.core.client.impl.ClientMessageImpl
import org.json4s.JsonAST.{JField, JObject}
import org.json4s.{CustomSerializer, DefaultFormats, Formats, _}

import scala.collection.immutable.Seq
import scala.concurrent.duration.Duration
/**
  * This object mostly imitates AWS SQS' API via an embedded HornetQ. See http://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/examples-sqs.html
  *
  * @author david
  * @since 7/18/17
  */
//todo in 1.23 all but the server side will use the client RemoteHornetQ implementation (which will call to the server at the hub)
//todo in 1.24, create an AwsSqs implementation of the trait

trait HornetQMom {
  def createQueueIfAbsent(queueName:String):Queue
  def deleteQueue(queueName:String)
  def queues:Seq[Queue]
  def send(contents:String,to:Queue):Unit
  def receive(from:Queue,timeout:Duration):Option[Message]
  def completeMessage(message:Message):Unit

}

case class Message(hornetQMessage:ClientMessage) extends DefaultJsonSupport {
  override implicit def json4sFormats: Formats = DefaultFormats
  val propName = "contents"

  def getClientMessage = hornetQMessage

  def contents = hornetQMessage.getStringProperty(propName)

  def getMessageID = hornetQMessage.getMessageID

  def complete() = hornetQMessage.acknowledge()
}

case class Queue(name:String) extends DefaultJsonSupport

class MessageSerializer extends CustomSerializer[Message](format => (
  {
    //JObject(List((hornetQMessage,JObject(List((type,JInt(0)), (durable,JBool(false)), (expiration,JInt(0)), (timestamp,JInt(1502218873012)), (priority,JInt(4)))))))
    // type, durable, expiration, timestamp, priority, initialMessageBufferSize
    case JObject(JField("hornetQMessage", JObject(JField("type", JInt(s)) :: JField("durable", JBool(d)) :: JField("expiration", JInt(e))
      :: JField("timestamp", JInt(t)) :: JField("priority", JInt(p)) :: Nil)) :: Nil) =>
      new Message(new ClientMessageImpl(s.toByte, d, e.toLong, t.toLong, p.toByte, 0))
  },
  {
    case msg: Message =>
      JObject(JField("hornetQMessage",
        JObject(JField("type", JLong(msg.getClientMessage.getType)) ::
          JField("durable", JBool(msg.getClientMessage.isDurable)) ::
          JField("expiration", JLong(msg.getClientMessage.getExpiration)) ::
          JField("timestamp", JLong(msg.getClientMessage.getTimestamp)) ::
          JField("priority", JLong(msg.getClientMessage.getPriority)) :: Nil)) :: Nil)
  }
))