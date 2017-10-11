package net.shrine.messagequeueservice

import net.shrine.source.ConfigSource
import net.shrine.spray.DefaultJsonSupport
import spray.http.StatusCode

import scala.collection.immutable.Seq
import scala.concurrent.duration.Duration
import scala.util.Try
/**
  * This API mostly imitates AWS SQS' API via an embedded HornetQ. See http://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/examples-sqs.html
  *
  * @author david
  * @since 7/18/17
  */
// in 1.23 all but the server side will use the client RemoteHornetQ implementation (which will call to the server at the hub)
//todo in 1.24, create an AwsSqs implementation of the trait

trait MessageQueueService {
  def createQueueIfAbsent(queueName:String): Try[Queue]
  def deleteQueue(queueName:String): Try[Unit]
  def queues: Try[Seq[Queue]]
  def send(contents:String,to:Queue): Try[Unit]
  def receive(from:Queue,timeout:Duration): Try[Option[Message]]
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


case class Queue(var name:String) extends DefaultJsonSupport {
  // filter all (Unicode) characters that are not letters
  // filter neither letters nor (decimal) digits, replaceAll("[^\\p{L}]+", "")
  name = name.filterNot(c => c.isWhitespace).replaceAll("[^\\p{L}\\p{Nd}]+", "")
  if (name.length == 0) {
    throw new IllegalArgumentException("ERROR: A valid Queue name must contain at least one letter!")
  }
}

case class CouldNotCompleteMomTaskButOKToRetryException(task:String,
                                                        status:Option[StatusCode] = None,
                                                        contents:Option[String] = None) extends Exception(s"Could not $task due to status code $status with message '$contents'")

case class CouldNotCompleteMomTaskException(task:String,
                                            status:Option[StatusCode] = None,
                                            queueName: String,
                                            contents:Option[String] = None) extends Exception(s"Could not $task from queue $queueName due to status code $status with message '$contents'")
