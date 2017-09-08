package net.shrine.messagequeueservice

import java.util.UUID

import net.shrine.source.ConfigSource
import net.shrine.spray.DefaultJsonSupport
import org.hornetq.api.core.SimpleString
import org.hornetq.api.core.client.{ClientConsumer, ClientMessage}
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
  def completeMessage(messageID:UUID): Try[Unit]

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

case class Message(messageUUID: UUID, contents: String) extends DefaultJsonSupport {
  override implicit def json4sFormats: Formats = DefaultFormats
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

case class NoSuchQueueExistsInHornetQ(proposedQueue: Queue) extends Exception {
  override def getMessage: String = {
    s"Given Queue ${proposedQueue.name} does not exist in HornetQ server! Please create the queue first!"
  }

}
