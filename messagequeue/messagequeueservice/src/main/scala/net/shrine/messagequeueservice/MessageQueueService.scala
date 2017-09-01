package net.shrine.messagequeueservice

import net.shrine.source.ConfigSource
import net.shrine.spray.DefaultJsonSupport
import org.hornetq.api.core.client.ClientMessage
import org.hornetq.core.client.impl.ClientMessageImpl
import org.json4s.JsonAST.{JField, JObject}
import org.json4s.{CustomSerializer, DefaultFormats, Formats, _}

import scala.collection.immutable.Seq
import scala.concurrent.duration.Duration
import scala.util.Try
/**
  * This object mostly imitates AWS SQS' API via an embedded HornetQ. See http://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/examples-sqs.html
  *
  * @author david
  * @since 7/18/17
  */
//todo in 1.23 all but the server side will use the client RemoteHornetQ implementation (which will call to the server at the hub)
//todo in 1.24, create an AwsSqs implementation of the trait

trait MessageQueueService {
  def createQueueIfAbsent(queueName:String): Try[Queue]
  def deleteQueue(queueName:String): Try[Unit]
  def queues: Try[Seq[Queue]]
  def send(contents:String,to:Queue): Try[Unit]
  def receive(from:Queue,timeout:Duration): Try[Option[Message]]
  def completeMessage(message:Message): Try[Unit]

}

object MessageQueueService {

  lazy val service:MessageQueueService = {
    import scala.reflect.runtime.universe.runtimeMirror

    val momClassName = ConfigSource.config.getString("shrine.messagequeue.implementation")
    val classLoaderMirror = runtimeMirror(getClass.getClassLoader)
    val module = classLoaderMirror.staticModule(momClassName)

    classLoaderMirror.reflectModule(module).instance.asInstanceOf[MessageQueueService]
  }
}

case class Message(hornetQMessage:ClientMessage) extends DefaultJsonSupport {
  override implicit def json4sFormats: Formats = DefaultFormats

  def getClientMessage = hornetQMessage

  def contents = hornetQMessage.getStringProperty(Message.contentsKey)

  def getMessageID = hornetQMessage.getMessageID

  def complete() = hornetQMessage.acknowledge()
}

object Message {
  val contentsKey = "contents"
}

case class Queue(var name:String) extends DefaultJsonSupport {
  // filter all (Unicode) characters that are not letters
  // filter neither letters nor (decimal) digits, replaceAll("[^\\p{L}]+", "")
  name = name.filterNot(c => c.isWhitespace).replaceAll("[^\\p{L}\\p{Nd}]+", "")
  if (name.length == 0) {
    throw new IllegalArgumentException("ERROR: A valid Queue name must contain at least one letter!")
  }
}

class QueueSerializer extends CustomSerializer[Queue](format => (
  {
    case JObject(JField("name", JString(s)) :: Nil) => Queue(s)
  },
  {
    case queue: Queue =>
      JObject(JField("name", JString(queue.name)) :: Nil)
  }
))

//todo make this an object
//todo move to HornetQMomWebApi
class MessageSerializer extends CustomSerializer[Message](format => (
  {

    //JObject(List((hornetQMessage,JObject(List((type,JInt(0)), (durable,JBool(true)), (expiration,JInt(0)), (timestamp,JInt(1504275142323)), (priority,JInt(4)), (contents,JString(test Content)))))))
    case JObject(
      JField("hornetQMessage", JObject(
        JField("type", JInt(s)) ::
        JField("durable", JBool(d)) ::
        JField("expiration", JInt(e)) ::
        JField("timestamp", JInt(t)) ::
        JField("priority", JInt(p)) ::
        JField(Message.contentsKey, JString(contents)) ::
        Nil)
      ) :: Nil ) => {
        val hornetQClientMessage = new ClientMessageImpl(s.toByte, d, e.toLong, t.toLong, p.toByte, 0)
        hornetQClientMessage.putStringProperty(Message.contentsKey,contents)

        Message(hornetQClientMessage)
      }
   },
  {
    case msg: Message =>
      JObject(
        JField("hornetQMessage",
        JObject(
          JField("type", JLong(msg.getClientMessage.getType)) ::
          JField("durable", JBool(msg.getClientMessage.isDurable)) ::
          JField("expiration", JLong(msg.getClientMessage.getExpiration)) ::
          JField("timestamp", JLong(msg.getClientMessage.getTimestamp)) ::
          JField("priority", JLong(msg.getClientMessage.getPriority)) ::
          JField(Message.contentsKey, JString(msg.contents)) ::
          Nil)) :: Nil)
  }
))


case class NoSuchQueueExistsInHornetQ(proposedQueue: Queue) extends Exception {
  override def getMessage: String = {
    s"Given Queue ${proposedQueue.name} does not exist in HornetQ server! Please create the queue first!"
  }

}
