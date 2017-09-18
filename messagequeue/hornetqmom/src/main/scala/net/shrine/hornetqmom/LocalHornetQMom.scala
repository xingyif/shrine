package net.shrine.hornetqmom

import java.util.concurrent.{BlockingDeque, LinkedBlockingDeque, TimeUnit}

import com.typesafe.config.Config
import net.shrine.config.ConfigExtensions
import net.shrine.log.Log
import net.shrine.messagequeueservice.{Message, MessageQueueService, NoSuchQueueExistsInHornetQ, Queue}
import net.shrine.source.ConfigSource
import org.hornetq.api.core.{HornetQQueueExistsException, TransportConfiguration}
import org.hornetq.api.core.client.{ClientConsumer, ClientMessage, ClientProducer, ClientSession, ClientSessionFactory, HornetQClient, ServerLocator}
import org.hornetq.api.core.management.HornetQServerControl
import org.hornetq.core.config.impl.ConfigurationImpl
import org.hornetq.core.remoting.impl.invm.{InVMAcceptorFactory, InVMConnectorFactory}
import org.hornetq.core.server.{HornetQServer, HornetQServers}

import scala.collection.concurrent.{TrieMap, Map => ConcurrentMap}
import scala.collection.immutable.Seq
import scala.concurrent.blocking
import scala.concurrent.duration.Duration
import scala.util.Try
/**
  * This object is the local version of the Message-Oriented Middleware API, which uses HornetQ service
  *
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

  val namesToShadowQueues:ConcurrentMap[String,BlockingDeque[String]] = TrieMap.empty

  //queue lifecycle
  def createQueueIfAbsent(queueName: String): Try[Queue] = {

    namesToShadowQueues.getOrElseUpdate(queueName, new LinkedBlockingDeque[String]())

    val proposedQueue: Queue = Queue(queueName)
    for {
      serverControl: HornetQServerControl <- Try{ hornetQServer.getHornetQServerControl }
      queuesSoFar <- queues
      queueToUse <- Try {
        queuesSoFar.find(_.name == proposedQueue.name).fold{
          try {
            serverControl.createQueue(proposedQueue.name, proposedQueue.name, true)
          } catch {
            case hqqex:HornetQQueueExistsException => Log.debug(s"Ignored a HornetQQueueExistsException in createQueueIfAbsent because $proposedQueue already exists.")
          }
          proposedQueue
        }{
          queue => queue}
      }
      consumer <- Try {
        queuesToConsumers.getOrElseUpdate(proposedQueue, { session.createConsumer(proposedQueue.name) })
      }
    } yield queueToUse
  }

  def deleteQueue(queueName: String): Try[Unit] = {
    namesToShadowQueues.remove(queueName)

    val proposedQueue: Queue = Queue(queueName)
    for {
      deleteTry <- Try {
        queuesToConsumers.remove(proposedQueue).foreach(_.close())
        val serverControl: HornetQServerControl = hornetQServer.getHornetQServerControl
        serverControl.destroyQueue(proposedQueue.name)
      }
    } yield deleteTry
  }

  override def queues: Try[Seq[Queue]] = {
    namesToShadowQueues.keys

    for {
      hornetQTry: HornetQServerControl <- Try {
        hornetQServer.getHornetQServerControl
      }
      getQueuesTry: Seq[Queue] <- Try {
        val queueNames: Array[String] = hornetQTry.getQueueNames
        queueNames.map(Queue(_)).to[Seq]
      }
    } yield getQueuesTry
  }

  //send a message
  def send(contents: String, to: Queue): Try[Unit] = {
    for {
      sendTry <- Try {
        // check if the queue exists first
        if (!this.queues.get.map(_.name).contains(to.name)) {
          throw NoSuchQueueExistsInHornetQ(to)
        }
      }
      producer: ClientProducer <- Try{ session.createProducer(to.name) }
      message <- Try{
        val msg = session.createMessage(true).putStringProperty(Message.contentsKey, contents)
        val messageTimeToLiveInMillis: Long = ConfigSource.config.get("shrine.messagequeue.hornetQWebApi.messageTimeToLive", Duration(_)).toMillis
        msg.setExpiration(System.currentTimeMillis() + messageTimeToLiveInMillis)
        msg
      }
      sendMessage <- Try {
        producer.send(message)
        Log.debug(s"Message $message sent to $to in HornetQ")
        producer.close()
      }
    } yield {
      val shadowQueue = namesToShadowQueues.get(to.name)
      shadowQueue.foreach(queue => queue.addLast(contents))
      Log.debug(s"After send to ${to.name} - shadowQueue ${shadowQueue.map(_.size)} ${shadowQueue.map(_.toString)}")

      sendMessage
    }
  }

  //receive a message
  /**
    * Always do AWS SQS-style long polling.
    * Be sure your code can handle receiving the same message twice.
    *
    * @return Some message before the timeout, or None
    */
  def receive(from: Queue, timeout: Duration): Try[Option[Message]] = {
    for {
    //todo handle the case where either stop or close has been called on something gracefully
      messageConsumer: ClientConsumer <- Try {
        if (!queuesToConsumers.contains(from)) {
          throw new NoSuchElementException(s"Given Queue ${from.name} does not exist in HornetQ server! Please create the queue first!")
        }
        queuesToConsumers(from)
      }

      message: Option[LocalHornetQMessage] <- Try {
        blocking {
          val messageReceived: Option[ClientMessage] = Option(messageConsumer.receive(timeout.toMillis))
          messageReceived.foreach(m => Log.debug(s"Received $m from $from in HornetQ"))
          messageReceived.map(clientMsg => LocalHornetQMessage(clientMsg))
        }
      }
    } yield {
      val shadowQueue = namesToShadowQueues.get(from.name)
      Log.debug(s"Before receive from ${from.name} - shadowQueue ${shadowQueue.map(_.size)} ${shadowQueue.map(_.toString)}")
//      shadowQueue.foreach(queue => Option(queue.pollFirst(timeout.toMillis,TimeUnit.MILLISECONDS)))
      val shadowMessage: Option[String] = shadowQueue.map(queue => Option(queue.poll())).flatten

      (shadowMessage,message) match {
        case (Some(sm),Some(m)) => //this is fine. Both have a message
        case (None,None) => //this is fine. Neither has a message
        case (Some(sm),None) => Log.error(s"A shadowMessage exists, but message is $message")//this is bad, and what I think is going on
        case (None,Some(m)) => Log.error(s"No shadowMessage exists for a message")//this is bad, but possibly not terrible if the shadowMessage was already polled
      }

      message
    }
  }

  def getQueueConsumer(queue: Queue): Try[ClientConsumer] = {
    for {
      messageConsumer: ClientConsumer <- Try {
        if (!queuesToConsumers.contains(queue)) {
          throw new NoSuchElementException(s"Given Queue ${queue.name} does not exist in HornetQ server! Please create the queue first!")
        }
        queuesToConsumers(queue)
      }
    } yield messageConsumer
  }

  //todo dead letter queue for all messages SHRINE-2261

  case class LocalHornetQMessage private(clientMessage: ClientMessage) extends Message {

    override def contents: String = clientMessage.getStringProperty(Message.contentsKey)

    //complete a message
    override def complete(): Try[Unit] = Try { clientMessage.acknowledge() }
  }
}

/**
  * If the configuration is such that HornetQ should have been started use this object to stop it
  */
object LocalHornetQMomStopper {

  def stop(): Unit = {
    if(ConfigSource.config.getBoolean("shrine.messagequeue.hornetQWebApi.enabled")) {
      LocalHornetQMom.stop()
    }
  }

}