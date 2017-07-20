package net.shrine.mom

import com.typesafe.config.Config
import net.shrine.source.ConfigSource
import org.hornetq.api.core.TransportConfiguration
import org.hornetq.api.core.client.{ClientMessage, ClientSession, HornetQClient}
import org.hornetq.core.config.impl.ConfigurationImpl
import org.hornetq.core.remoting.impl.invm.{InVMAcceptorFactory, InVMConnectorFactory}
import org.hornetq.core.server.HornetQServers

import scala.concurrent.blocking
import scala.concurrent.duration.Duration
/**
  * This object mostly imitates AWS SQS' API via an embedded HornetQ. See http://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/examples-sqs.html
  *
  * @author david 
  * @since 7/18/17
  */
//todo a better name
//todo split into a trait, LocalHornetQ, and RemoteHornetQ versions
//todo in 1.24, create an AwsSqs implementation of the trait
object HornetQMom {

  val config:Config = ConfigSource.config.getConfig("shrine.hub.mom.hornetq")

  // todo use the config to set everything needed here that isn't hard-coded.
  val hornetQConfiguration = new ConfigurationImpl()
  // todo from config?
  hornetQConfiguration.setJournalDirectory("target/data/journal")
  // todo want this. There are likely many other config bits
  hornetQConfiguration.setPersistenceEnabled(false)
  // todo maybe want this
  hornetQConfiguration.setSecurityEnabled(false)
  // todo probably just want the InVM version, but need to read up on options
  hornetQConfiguration.getAcceptorConfigurations.add(new TransportConfiguration(classOf[InVMAcceptorFactory].getName))

  // Create and start the server
  val hornetQServer = HornetQServers.newHornetQServer(hornetQConfiguration)
  hornetQServer.start()

  // Step 3. As we are not using a JNDI environment we instantiate the objects directly
  val serverLocator = HornetQClient.createServerLocatorWithoutHA(new TransportConfiguration(classOf[InVMConnectorFactory].getName))
  val sessionFactory = serverLocator.createSessionFactory()

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
    //todo do we want any of these to be true? definitely want autoCommitSends val session: ClientSession = sessionFactory.createSession(false, false, false)
    val session: ClientSession = sessionFactory.createSession()
    try {

      block(session)
    }
    finally {
      session.close()
    }
  }

  //queue lifecycle
  //todo if absent ?
  def createQueueIfAbsent(queueName:String):Queue = withSession{ session =>
    session.createQueue(queueName, queueName, true)
    Queue(queueName)
  }

  def deleteQueue(queueName:String) = withSession { session =>
    session.deleteQueue(queueName)
  }

  def queues:Seq[Queue] = withSession { session =>
    ??? //todo how to do this?
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
      messageReceived.map(m => Message(m.getStringProperty(propName)))
    }
  }

  //todo dead letter queue for all messages. See http://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/examples-sqs-dead-letter-queues.html

  //complete a message
  def complete(message:Message):Unit = ???

  case class Queue(name:String)

  case class Message(contents:String)

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
