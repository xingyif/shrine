package net.shrine.mom

import java.util.Date

import org.hornetq.api.core.TransportConfiguration
import org.hornetq.api.core.client.HornetQClient
import org.hornetq.core.remoting.impl.invm.InVMConnectorFactory
import org.junit.runner.RunWith
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.junit.JUnitRunner
import org.scalatest.{BeforeAndAfter, FlatSpec, Matchers}

/**
  * Test creation, insertion, querying, and deletion of ProblemDigest values into an
  * in-memory H2 database. Demonstrates proof of concept mapping of ProblemDigest
  * case class into a database.
  */
@RunWith(classOf[JUnitRunner])
class HornetQMomTest extends FlatSpec with BeforeAndAfter with ScalaFutures with Matchers {

  "HornetQ" should "be able to send and receive a messages" in {

    //todo hack to bring HornetQMom into the VM. Remove it when you can
    HornetQMom.toString

    // Step 3. As we are not using a JNDI environment we instantiate the objects directly
    val serverLocator = HornetQClient.createServerLocatorWithoutHA(new TransportConfiguration(classOf[InVMConnectorFactory].getName))
    val sf = serverLocator.createSessionFactory()

    // Step 4. Create a core queue
    val coreSession = sf.createSession(false, false, false)

    val queueName = "queue.exampleQueue"

    coreSession.createQueue(queueName, queueName, true)

    coreSession.close()

    // Step 5. Create the session, and producer
    val session = sf.createSession()

    val producer = session.createProducer(queueName)

    // Step 6. Create and send a message
    val message = session.createMessage(false)

    val propName = "myprop"

    message.putStringProperty(propName, "Hello sent at " + new Date())

    System.out.println("Sending the message.")

    producer.send(message)

    // Step 7. Create the message consumer and start the connection
    val messageConsumer = session.createConsumer(queueName)
    session.start()

    // Step 8. Receive the message.
    val  messageReceived = messageConsumer.receive(1000)
    System.out.println("Received TextMessage:" + messageReceived.getStringProperty(propName))

    // Step 9. Be sure to close our resources!
    if (sf != null)
    {
      sf.close()
    }

    HornetQMom.stop()
  }

}
