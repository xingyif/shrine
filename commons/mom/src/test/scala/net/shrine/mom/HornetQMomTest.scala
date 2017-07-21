package net.shrine.mom

import java.util.Date

import net.shrine.mom.HornetQMom.Message
import org.hornetq.api.core.client.{ClientSession, ClientSessionFactory}
import org.junit.runner.RunWith
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.junit.JUnitRunner
import org.scalatest.{BeforeAndAfter, FlatSpec, Ignore, Matchers}

import scala.concurrent.duration._

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

    val testContents = "Test message"
    HornetQMom.send(testContents,queue)

    val message: Option[Message] = HornetQMom.receive(queue,1 second)

    assert(message.isDefined)
    assert(message.get.contents == testContents)

    message.fold()(HornetQMom.complete)

//todo why doesn't deleteQueue work?   HornetQMom.deleteQueue(queueName)
    HornetQMomStopper.stop()
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
