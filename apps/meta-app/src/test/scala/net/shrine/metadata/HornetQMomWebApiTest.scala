import akka.actor.ActorRefFactory
import net.shrine.metadata.HornetQMomWebApi
import net.shrine.mom.{LocalHornetQMom, Message, MessageSerializer, Queue}
import org.json4s.NoTypeHints
import org.json4s.native.Serialization
import org.json4s.native.Serialization.{read, write}
import org.junit.runner.RunWith
import org.scalatest.FlatSpec
import org.scalatest.junit.JUnitRunner
import spray.http.HttpEntity
import spray.http.StatusCodes._
import spray.testkit.ScalatestRouteTest

import scala.concurrent.duration._
/**
  * Created by yifan on 7/27/17.
  */


@RunWith(classOf[JUnitRunner])
class HornetQMomWebApiTest extends FlatSpec with ScalatestRouteTest with HornetQMomWebApi {
  override def actorRefFactory: ActorRefFactory = system

  private val queueName = "testQueue"
  private val messageContent = "testContent"
  private var receivedMessage: String = ""

  "RemoteHornetQMom" should "create/delete the given queue, send/receive message, get queues" in {

    Put(s"/createQueue/$queueName") ~> routes ~> check {
      assertResult(Created)(status)
    }

    Put(s"/sendMessage/$messageContent/$queueName") ~> routes ~> check {
      assertResult(Accepted)(status)
    }

    // given timeout is 2 seconds
    Get(s"/receiveMessage/$queueName?timeOutDuration=2") ~> routes ~> check {
      val response = new String(body.data.toByteArray)

      val timeout: Duration = Duration.create(2, "seconds")
      val msg: Message = LocalHornetQMom.receive(Queue.apply(queueName), timeout).get
      implicit val formats = Serialization.formats(NoTypeHints) + new MessageSerializer
      val msgJSON = write[Message](msg)(formats)

      assertResult(OK)(status)
      assertResult(response)(msgJSON)
    }


    // default timeout is 20 seconds
    Get(s"/receiveMessage/$queueName") ~> routes ~> check {
      val response = new String(body.data.toByteArray)

      val timeout: Duration = Duration.create(20, "seconds")
      val msg: Message = LocalHornetQMom.receive(Queue.apply(queueName), timeout).get
      implicit val formats = Serialization.formats(NoTypeHints) + new MessageSerializer
      val msgJSON = write[Message](msg)(formats)

      assertResult(OK)(status)
      assertResult(response)(msgJSON)
    }

    // 0sec for an immediate return
    Get(s"/receiveMessage/$queueName?timeOutDuration=0") ~> routes ~> check {
      val response = new String(body.data.toByteArray)

      val timeout: Duration = Duration.create(0, "seconds")
      val msg: Message = LocalHornetQMom.receive(Queue.apply(queueName), timeout).get
      implicit val formats = Serialization.formats(NoTypeHints) + new MessageSerializer

      val msgJSON: String = write[Message](msg)(formats)

      receivedMessage = response

      assertResult(OK)(status)
      assertResult(response)(msgJSON)
    }

    Get(s"/getQueues") ~> routes ~> check {

      val allQueues = LocalHornetQMom.queues
      val size = allQueues.size
      println(s"queues in test: $size")

      implicit val formats = Serialization.formats(NoTypeHints) + new MessageSerializer
//      val queues: String = write(allQueues)(formats)
      val response = new String(body.data.toByteArray)
//      println("response: " + response)
      assertResult(OK)(status)
      println(s"getQueues: $response")
    }

    Put("/acknowledge", HttpEntity(s"""$receivedMessage""")) ~>
      routes ~> check {
      implicit val formats = Serialization.formats(NoTypeHints) + new MessageSerializer
      assertResult(NoContent)(status)
    }

    Put(s"/deleteQueue/$queueName") ~> routes ~> check {
      assertResult(OK)(status)
    }
  }
}