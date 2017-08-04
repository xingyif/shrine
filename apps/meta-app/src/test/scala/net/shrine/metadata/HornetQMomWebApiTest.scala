import akka.actor.ActorRefFactory
import net.shrine.metadata.HornetQMomWebApi
import net.shrine.mom.{LocalHornetQMom, Message, Queue}
import org.json4s.NoTypeHints
import org.json4s.native.Serialization
import org.json4s.native.Serialization.write
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
class HornetQMomWebApiTest extends FlatSpec with ScalatestRouteTest with HornetQMomWebApi {
  override def actorRefFactory: ActorRefFactory = system

  "RemoteHornetQMom" should "create/delete the given queue, send/receive message, get queues" in {

    Put(s"/createQueue/firstQueue") ~> routes ~> check {
      assertResult(Created)(status)
    }

    Put(s"/sendMessage/hello/firstQueue") ~> routes ~> check {
      assertResult(Accepted)(status)
    }

    // given timeout is 2 seconds
    Get(s"/receiveMessage/firstQueue?timeOutDuration=2") ~> routes ~> check {
      val response = new String(body.data.toByteArray)

      val timeout: Duration = Duration.create(2, "seconds")
      val msg: Message = LocalHornetQMom.receive(Queue.apply("firstQueue"), timeout).get
      implicit val formats = Serialization.formats(NoTypeHints)
      val msgJSON = write(msg)

      assertResult(OK)(status)
      assertResult(response)(msgJSON)
    }


    // default timeout is 20 seconds
    Get(s"/receiveMessage/firstQueue") ~> routes ~> check {
      val response = new String(body.data.toByteArray)

      val timeout: Duration = Duration.create(20, "seconds")
      val msg: Message = LocalHornetQMom.receive(Queue.apply("firstQueue"), timeout).get
      implicit val formats = Serialization.formats(NoTypeHints)
      val msgJSON = write(msg)

      assertResult(OK)(status)
      assertResult(response)(msgJSON)
    }

    // 0sec for an immediate return
    Get(s"/receiveMessage/firstQueue?timeOutDuration=0") ~> routes ~> check {
      val response = new String(body.data.toByteArray)

      val timeout: Duration = Duration.create(0, "seconds")
      val msg: Message = LocalHornetQMom.receive(Queue.apply("firstQueue"), timeout).get
      implicit val formats = Serialization.formats(NoTypeHints)
      val msgJSON = write(msg)

      assertResult(OK)(status)
      assertResult(response)(msgJSON)
    }

    Get(s"/getQueues") ~> routes ~> check {

      val allQueues = LocalHornetQMom.queues

      implicit val formats = Serialization.formats(NoTypeHints)
      val queues = write(allQueues)
      val response = new String(body.data.toByteArray)
      assertResult(OK)(status)
    }

    Put(s"/acknowledge/4") ~> routes ~> check {
      assertResult(NoContent)(status)
    }


    Put(s"/deleteQueue/firstQueue") ~> routes ~> check {
      assertResult(OK)(status)
    }
  }
}