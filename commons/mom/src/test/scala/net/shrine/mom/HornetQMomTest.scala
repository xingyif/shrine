package net.shrine.mom

import java.util.Date

import org.hornetq.api.core.client.{ClientSession, ClientSessionFactory}
import org.junit.runner.RunWith
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.junit.JUnitRunner
import org.scalatest.{BeforeAndAfter, FlatSpec, Ignore, Matchers}

/**
  * Test creation, insertion, querying, and deletion of ProblemDigest values into an
  * in-memory H2 database. Demonstrates proof of concept mapping of ProblemDigest
  * case class into a database.
  */
@RunWith(classOf[JUnitRunner])
class HornetQMomTest extends FlatSpec with BeforeAndAfter with ScalaFutures with Matchers {

  "HornetQ" should "be able to send and receive a message" in {

    val queueName = "testQueue"
    val queue = HornetQMom.createQueueIfAbsent(queueName)

    val sf: ClientSessionFactory = HornetQMom.sessionFactory

    val propName = "contents"

    HornetQMom.send("Test",queue)

    HornetQMom.withSession { session =>

      // Step 7. Create the message consumer and start the connection
      val messageConsumer = session.createConsumer(queueName)
      session.start()

      // Step 8. Receive the message.
      val messageReceived = messageConsumer.receive(1000)

      assert(null != messageReceived)

      System.out.println("Received TextMessage:" + messageReceived.getStringProperty(propName))
    }
    // Step 9. Be sure to close our resources!
    if (sf != null)
    {
      sf.close()
    }

//todo why doesn't this work?   HornetQMom.deleteQueue(queueName)
    //todo HornetQMomStopper.stop()
  }

/*
  "HornetQ" should "be OK if asked to create the same queue twice " in {

    val queueName = "testQueue"
    HornetQMom.createQueueIfAbsent(queueName)
    HornetQMom.createQueueIfAbsent(queueName)

    //todo why doesn't this work?    HornetQMom.deleteQueue(queueName)
    //todo HornetQMomStopper.stop()
  }
*/
}
