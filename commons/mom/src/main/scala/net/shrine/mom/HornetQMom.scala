package net.shrine.mom

import com.typesafe.config.Config
import net.shrine.source.ConfigSource
import org.hornetq.api.core.{HornetQQueueExistsException, TransportConfiguration}
import org.hornetq.api.core.client.{ClientMessage, ClientSession, ClientSessionFactory, HornetQClient, ServerLocator}
import org.hornetq.api.core.management.HornetQServerControl
import org.hornetq.core.config.impl.ConfigurationImpl
import org.hornetq.core.remoting.impl.invm.{InVMAcceptorFactory, InVMConnectorFactory}
import org.hornetq.core.server.{HornetQServer, HornetQServers}

import scala.concurrent.blocking
import scala.concurrent.duration.Duration
import scala.collection.immutable.Seq

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
object HornetQMom {

  val config:Config = ConfigSource.config.getConfig("shrine.hub.mom.hornetq")

  // todo use the config to set everything needed here that isn't hard-coded.
  val hornetQConfiguration = new ConfigurationImpl()
  // todo from config? What is the journal file about? If temporary, use a Java temp file.
  hornetQConfiguration.setJournalDirectory("target/data/journal")
  // todo want this. There are likely many other config bits
  hornetQConfiguration.setPersistenceEnabled(false)
  // todo maybe want this
  hornetQConfiguration.setSecurityEnabled(false)
  // todo probably just want the InVM version, but need to read up on options
  hornetQConfiguration.getAcceptorConfigurations.add(new TransportConfiguration(classOf[InVMAcceptorFactory].getName))

  // Create and start the server
  val hornetQServer: HornetQServer = HornetQServers.newHornetQServer(hornetQConfiguration)
  hornetQServer.start()

  val serverLocator: ServerLocator = HornetQClient.createServerLocatorWithoutHA(new TransportConfiguration(classOf[InVMConnectorFactory].getName))

  val sessionFactory: ClientSessionFactory = serverLocator.createSessionFactory()

  val propName = "contents"

  /**
    * Use HornetQMomStopper to stop the hornetQServer without unintentially starting it
    */
  private[mom] def stop() = {
    sessionFactory.close()
    hornetQServer.stop()
  }

  private def withSession[T](block: ClientSession => T):T = {
    //arguments are boolean xa, boolean autoCommitSends, boolean autoCommitAcks .
    //todo do we want to use any of the createSession parameters?
    val session: ClientSession = sessionFactory.createSession()
    try {
      block(session)
    }
    finally {
      session.close()
    }
  }

  //queue lifecycle
  def createQueueIfAbsent(queueName:String):Queue = {
    val serverControl: HornetQServerControl = hornetQServer.getHornetQServerControl
    if(!queues.map(_.name).contains(queueName)) {
      try serverControl.createQueue(queueName, queueName) //todo how is the address (first argument) used? I'm just throwing in the queue name. Seems to work but why?
      catch {
        case alreadyExists: HornetQQueueExistsException => //already has what we want
      }
    }
    Queue(queueName)
  }

  def deleteQueue(queueName:String) = {
    val serverControl: HornetQServerControl = hornetQServer.getHornetQServerControl
    serverControl.destroyQueue(queueName)
  }

  def queues:Seq[Queue] = {
    val serverControl: HornetQServerControl = hornetQServer.getHornetQServerControl
    val queueNames: Array[String] = serverControl.getQueueNames
    queueNames.map(Queue(_)).to[Seq]
  }

  //send a message
  def send(contents:String,to:Queue):Unit =  withSession{ session =>
    val producer = session.createProducer(to.name)
    val message = session.createMessage(false)
    message.putStringProperty(propName, contents)

    producer.send(message)
  }

  //receive a message
  /**
    * Always do AWS SQS-style long polling.
    * Be sure your code can handle receiving the same message twice.
    *
    * @return Some message before the timeout, or None
    */
  def receive(from:Queue,timeout:Duration):Option[Message] = withSession{ session =>
    val messageConsumer = session.createConsumer(from.name)
    session.start()
    blocking {
      val messageReceived: Option[ClientMessage] = Option(messageConsumer.receive(timeout.toMillis))
      messageReceived.map(Message(_))
    }
  }

  //todo dead letter queue for all messages. See http://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/examples-sqs-dead-letter-queues.html

  //complete a message
  //todo better here or on the message itself??
  //todo if we can find API that takes a message ID instead of the message. Otherwise its a state puzzle for the web server implementation
  def complete(message:Message):Unit = message.complete()

  case class Queue(name:String)

  case class Message(hornetQMessage:ClientMessage) {
    def contents = hornetQMessage.getStringProperty(propName)

    def complete() = hornetQMessage.acknowledge()
  }

}

/**
  * If the configuration is such that HornetQ should have been started use this object to stop it
  */
//todo is this a good way to write this code?
object HornetQMomStopper {

  def stop() = {
    //todo fill in as part of SHIRINE-2128
    val config: Config = ConfigSource.config.getConfig("shrine.hub.mom.hornetq")

    HornetQMom.stop()
  }

}
