package net.shrine.mom

import net.shrine.mom.HornetQMom.Message
import org.junit.runner.RunWith
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.junit.JUnitRunner
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, FlatSpec, Matchers}

import scala.concurrent.duration._
import scala.language.postfixOps

/**
  * Test creation, insertion, querying, and deletion of ProblemDigest values into an
  * in-memory H2 database. Demonstrates proof of concept mapping of ProblemDigest
  * case class into a database.
  */
@RunWith(classOf[JUnitRunner])
class HornetQMomTest extends FlatSpec with BeforeAndAfterAll with ScalaFutures with Matchers {

  "HornetQ" should "be able to send and receive a message" in {

    val queueName = "testQueue"

    assert(HornetQMom.queues.isEmpty)

    val queue = HornetQMom.createQueueIfAbsent(queueName)

    assert(HornetQMom.queues == Seq(queue))


    val testContents = "Test message"
    HornetQMom.send(testContents,queue)

    val message: Option[Message] = HornetQMom.receive(queue,1 second)

    assert(message.isDefined)
    assert(message.get.contents == testContents)

    message.fold()(HornetQMom.complete)

    HornetQMom.deleteQueue(queueName)
    assert(HornetQMom.queues.isEmpty)

  }


  "HornetQ" should "be OK if asked to create the same queue twice " in {

    val queueName = "testQueue"
    HornetQMom.createQueueIfAbsent(queueName)
    HornetQMom.createQueueIfAbsent(queueName)

    HornetQMom.deleteQueue(queueName)
  }

  override def afterAll() = HornetQMomStopper.stop()
}
