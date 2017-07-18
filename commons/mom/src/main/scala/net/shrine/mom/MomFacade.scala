package net.shrine.mom

import org.hornetq.api.core.TransportConfiguration
import org.hornetq.api.core.client.HornetQClient

import scala.concurrent.duration.Duration
import scala.concurrent.blocking
import org.hornetq.core.config.impl.ConfigurationImpl
import org.hornetq.core.remoting.impl.invm.{InVMAcceptorFactory, InVMConnectorFactory}
import org.hornetq.core.server.HornetQServers;
/**
  * This object mostly imitates AWS SQS' API via an embedded HornetQ. See http://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/examples-sqs.html
  *
  * @author david 
  * @since 7/18/17
  */
//todo a better name
//todo define a trait, LocalHornetQ, and RemoteHornetQ versions
object HornetQMom {

  //queue lifecycle
  def createQueueIfAbasent(queueName:String):Queue = {
???
  }

  def deleteQueue(queueName:String) = ???

  def queues:Seq[Queue] = ???

  //send a message
  def send(messageBody:String,to:Queue):Unit = ???

  //receive a message
  /**
    * Always do AWS SQS-style long polling.
    * Be sure your code can handle receiving the same message twice.
    *
    * @return Some message before the timeout, or None
    */
  def receive(from:Queue,timeout:Duration):Option[Message] = blocking {
    ???
  }

  //todo dead letter queue for all messages. See http://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/examples-sqs-dead-letter-queues.html

  //complete a message
  def complete(message:Message):Unit = ???

  case class Queue(name:String)

  case class Message(contents:String)

}
