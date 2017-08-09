package net.shrine.mom

import org.junit.runner.RunWith
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.junit.JUnitRunner
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}
import scala.collection.immutable.Seq
import scala.concurrent.duration._
import scala.language.postfixOps

/**
  * Test creation, insertion, querying, and deletion of ProblemDigest values into an
  * in-memory H2 database. Demonstrates proof of concept mapping of ProblemDigest
  * case class into a database.
  */
@RunWith(classOf[JUnitRunner])
class LocalHornetQMomTest extends FlatSpec with BeforeAndAfterAll with ScalaFutures with Matchers {

  "LocalHornetQ" should "be able to send and receive a message" in {

    val queueName = "testQueue"

    assert(LocalHornetQMom.queues.isEmpty)

    val queue = LocalHornetQMom.createQueueIfAbsent(queueName)

    assert(LocalHornetQMom.queues == Seq(queue))

    val testContents = "Test message"
    LocalHornetQMom.send(testContents,queue)

    val message: Option[Message] = LocalHornetQMom.receive(queue,1 second)

    assert(message.isDefined)
    assert(message.get.contents == testContents)

    LocalHornetQMom.completeMessage(message.get)

    // todo merge localHornetQMom with Dave's pr-2155, this will be fixed
    //  val nonMessage: Option[Message] = LocalHornetQMom.receive(queue,1 second)

    //  assert(nonMessage.isEmpty)


    LocalHornetQMom.deleteQueue(queueName)
    assert(LocalHornetQMom.queues.isEmpty)

  }


  "HornetQ" should "be OK if asked to create the same queue twice " in {

    val queueName = "testQueue"
    LocalHornetQMom.createQueueIfAbsent(queueName)
    LocalHornetQMom.createQueueIfAbsent(queueName)

    LocalHornetQMom.deleteQueue(queueName)
  }

  override def afterAll() = LocalHornetQMomStopper.stop()
}
