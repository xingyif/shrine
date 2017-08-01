import akka.actor.ActorRefFactory
import net.shrine.metadata.RemoteHornetQMom
import net.shrine.mom.Message
import org.junit.runner.RunWith
import org.scalatest.FlatSpec
import org.scalatest.junit.JUnitRunner
import spray.http.StatusCodes._
import spray.testkit.ScalatestRouteTest

import scala.concurrent.duration._
/**
  * Created by yifan on 7/27/17.
  */


@RunWith(classOf[JUnitRunner])
class RemoteHornetQMomTest extends FlatSpec with ScalatestRouteTest with RemoteHornetQMom {
  override def actorRefFactory: ActorRefFactory = system


  "RemoteHornetQ" should "be able to send and receive a message" in {

    val queueName = "testQueue"

    assert(this.queues.isEmpty)

    val queue = this.createQueueIfAbsent(queueName)

    assert(this.queues == Seq(queue))

    val testContents = "Test message"
    this.send(testContents,queue)

    val message: Option[Message] = this.receive(queue, 1 second)

    assert(message.isDefined)
    assert(message.get.contents == testContents)
    this.deleteQueue(queueName)
    assert(this.queues.isEmpty)
  }


  "RemoteHornetQMom" should "create the given queue and return StatusCodes.Created" in {

    Get(s"/createQueue?queueName=firstQueue") ~> routes ~> check {
      assertResult(Created)(status)
    }
  }

  "RemoteHornetQMom" should "send message with the given content to the given queue and return an OK" in {

    Get(s"/sendMessage?messageContent=hello&toQueue=firstQueue") ~> routes ~> check {
      assertResult(Accepted)(status)
    }
  }

  "RemoteHornetQMom" should "receive a message from the given queue with the given timeout and return an OK" in {

    Get(s"/receiveMessage?fromQueue=firstQueue&timeOutDuration=1sec") ~> routes ~> check {
      assertResult(OK)(status)
    }
  }

  "RemoteHornetQMom" should "acknowledge and return success code with no content" in {

    Get(s"/acknowledge?message=messageReceived") ~> routes ~> check {
      assertResult(NoContent)(status)
    }
  }

  "RemoteHornetQMom" should "delete the given queue and return an OK" in {

    Get(s"/deleteQueue?queueName=firstQueue") ~> routes ~> check {
      assertResult(OK)(status)
    }
  }

  "RemoteHornetQMom" should "get all queues and return an OK" in {

    Get(s"/getQueues") ~> routes ~> check {
      assertResult(OK)(status)
    }
  }

}