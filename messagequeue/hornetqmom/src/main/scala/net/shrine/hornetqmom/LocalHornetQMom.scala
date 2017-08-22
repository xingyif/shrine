package net.shrine.hornetqmom

import com.typesafe.config.Config
import net.shrine.messagequeueservice.{Message, MessageQueueService, Queue}
import net.shrine.source.ConfigSource
import org.hornetq.api.core.client.{ClientConsumer, ClientMessage, ClientProducer, ClientSession, ClientSessionFactory, HornetQClient, ServerLocator}
import org.hornetq.api.core.management.HornetQServerControl
import org.hornetq.api.core.{HornetQQueueExistsException, TransportConfiguration}
import org.hornetq.core.config.impl.ConfigurationImpl
import org.hornetq.core.remoting.impl.invm.{InVMAcceptorFactory, InVMConnectorFactory}
import org.hornetq.core.server.{HornetQServer, HornetQServers}

import scala.collection.immutable.Seq
import scala.concurrent.blocking
import scala.concurrent.duration.Duration
import scala.collection.concurrent.{TrieMap, Map => ConcurrentMap}
import scala.util.Try
/**
  * This object is the local version of the Message-Oriented Middleware API, which uses HornetQ service
  * @author david
  * @since 7/18/17
  */

object LocalHornetQMom extends MessageQueueService {

  val config: Config = ConfigSource.config.getConfig("shrine.messagequeue.hornetq")

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

  //arguments are boolean xa, boolean autoCommitSends, boolean autoCommitAcks .
  val session: ClientSession = sessionFactory.createSession(false, true, true)
  session.start()

  //keep a map of live queues to ClientConsumers to provide a path for completing messages
  val queuesToConsumers: ConcurrentMap[Queue, ClientConsumer] = TrieMap.empty

  val propName = "contents"

  /**
    * Use HornetQMomStopper to stop the hornetQServer without unintentially starting it
    */
  // todo drop this into a try
  private[hornetqmom] def stop() = {
    queuesToConsumers.values.foreach(_.close())

    session.close()
    sessionFactory.close()
    hornetQServer.stop()
  }

  //queue lifecycle
  def createQueueIfAbsent(queueName: String): Queue = {
    val serverControl: HornetQServerControl = hornetQServer.getHornetQServerControl
    val createQueueTry: Try[Queue] = for {
      createQueueInHornetQ <- Try {
        if (!queues.map(_.name).contains(queueName)) {
          serverControl.createQueue(queueName, queueName, true)
        }
      }
      useNewQueueToUpdateMapTry: Queue <- Try {
        val newQueue: Queue = Queue(queueName)
        queuesToConsumers.getOrElseUpdate(newQueue, { session.createConsumer(newQueue.name) })
        newQueue
      }
    } yield useNewQueueToUpdateMapTry
    createQueueTry.get
  }

  def deleteQueue(queueName: String): Unit = {
    for {
      deleteTry <- Try {
        queuesToConsumers.remove(Queue(queueName)).foreach(_.close())
        val serverControl: HornetQServerControl = hornetQServer.getHornetQServerControl
        serverControl.destroyQueue(queueName)
      }
    } yield deleteTry
  }

  override def queues: Seq[Queue] = {
    val getQueuesTry: Try[Seq[Queue]] = for {
      hornetQTry: HornetQServerControl <- Try {
        // val serverControl: HornetQServerControl = hornetQServer.getHornetQServerControl
        hornetQServer.getHornetQServerControl
      }
      getQueuesTry: Seq[Queue] <- Try {
        val queueNames: Array[String] = hornetQTry.getQueueNames
        queueNames.map(Queue(_)).to[Seq]
      }
    } yield getQueuesTry
    getQueuesTry.get
  }

  //send a message
  def send(contents: String, to: Queue): Unit = {
    for {
      sendTry <- Try {
        val producer: ClientProducer = session.createProducer(to.name)
        val message = session.createMessage(true)
        message.putStringProperty(propName, contents)

        producer.send(message)

        producer.close()
      }
    } yield sendTry
  }

  //receive a message
  /**
    * Always do AWS SQS-style long polling.
    * Be sure your code can handle receiving the same message twice.
    *
    * @return Some message before the timeout, or None
    */
  def receive(from: Queue, timeout: Duration): Option[Message] = {
    if (!queuesToConsumers.contains(from)) {
      throw new NoSuchElementException(s"Given Queue ${from.name} does not exist in HornetQ server! Please create the queue first!")
    }
    val receiveMessageTry: Try[Option[Message]] = for {
    //todo handle the case where either stop or close has been called on something gracefully
      messageConsumer: ClientConsumer <- Try {
        queuesToConsumers(from)
      }
      message: Option[Message] <- Try {
        blocking {
          val messageReceived: Option[ClientMessage] = Option(messageConsumer.receive(timeout.toMillis))
          messageReceived.map(Message(_))
        }
      }
    } yield message
    receiveMessageTry.get
  }

  //todo dead letter queue for all messages. See http://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/examples-sqs-dead-letter-queues.html

  //complete a message
  override def completeMessage(message: Message): Unit = {
    for {
      completeMessageTry <- Try { message.complete() }
    } yield completeMessageTry
  }

}

/**
  * If the configuration is such that HornetQ should have been started use this object to stop it
  */
//todo is this a good way to write this code?
object LocalHornetQMomStopper {

  def stop() = {
    //todo fill in as part of SHIRINE-2128
    val config: Config = ConfigSource.config.getConfig("shrine.messagequeue.hornetq")

    LocalHornetQMom.stop()
  }

}