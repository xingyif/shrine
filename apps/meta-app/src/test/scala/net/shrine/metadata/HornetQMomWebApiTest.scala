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

    Put(s"/mom/createQueue/$queueName") ~> momRoute ~> check {
      val response = new String(body.data.toByteArray)
      implicit val formats = Serialization.formats(NoTypeHints)
      val jsonToQueue = read[Queue](response)(formats, manifest[Queue])
      val responseQueueName = jsonToQueue.name
      assertResult(Created)(status)
      assertResult(queueName)(responseQueueName)
    }

    Put(s"/mom/sendMessage/$messageContent/$queueName") ~> momRoute ~> check {
      assertResult(Accepted)(status)
    }

    Get(s"/mom/getQueues") ~> momRoute ~> check {

      val allQueues = LocalHornetQMom.queues

      implicit val formats = Serialization.formats(NoTypeHints)
      //      val queues: String = write(allQueues)(formats)
      val response: String = new String(body.data.toByteArray)
      val jsonToSeq: Seq[Queue] = read[Seq[Queue]](response, false)(formats, manifest[Seq[Queue]])

      assertResult(OK)(status)
      assertResult(queueName)(jsonToSeq.apply(0).name)
    }

    // given timeout is 2 seconds
    Get(s"/mom/receiveMessage/$queueName?timeOutDuration=2") ~> momRoute ~> check {
      val response = new String(body.data.toByteArray)

      val timeout: Duration = Duration.create(2, "seconds")
      val msg: Message = LocalHornetQMom.receive(Queue.apply(queueName), timeout).get
      implicit val formats = Serialization.formats(NoTypeHints) + new MessageSerializer
      val msgJSON = write[Message](msg)(formats)

      assertResult(OK)(status)
      assertResult(response)(msgJSON)
    }

    // default timeout is 20 seconds
    Get(s"/mom/receiveMessage/$queueName") ~> momRoute ~> check {
      val response = new String(body.data.toByteArray)

      val timeout: Duration = Duration.create(20, "seconds")
      val msg: Message = LocalHornetQMom.receive(Queue.apply(queueName), timeout).get
      implicit val formats = Serialization.formats(NoTypeHints) + new MessageSerializer
      val msgJSON = write[Message](msg)(formats)

      assertResult(OK)(status)
      assertResult(response)(msgJSON)
    }

    // 0sec for an immediate return
    Get(s"/mom/receiveMessage/$queueName?timeOutDuration=0") ~> momRoute ~> check {
      val response = new String(body.data.toByteArray)

      val timeout: Duration = Duration.create(0, "seconds")
      val msg: Message = LocalHornetQMom.receive(Queue.apply(queueName), timeout).get
      implicit val formats = Serialization.formats(NoTypeHints) + new MessageSerializer

      val msgJSON: String = write[Message](msg)(formats)

      receivedMessage = response

      assertResult(OK)(status)
      assertResult(response)(msgJSON)
    }

    Put("/mom/acknowledge", HttpEntity(s"""$receivedMessage""")) ~>
      momRoute ~> check {
      implicit val formats = Serialization.formats(NoTypeHints) + new MessageSerializer
      assertResult(NoContent)(status)
    }

    Put(s"/mom/deleteQueue/$queueName") ~> momRoute ~> check {
      assertResult(OK)(status)
    }
  }
}