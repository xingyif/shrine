import akka.actor.ActorRefFactory
import net.shrine.metadata.HornetQMomWebService
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
class HornetQMomWebServiceTest extends FlatSpec with ScalatestRouteTest with HornetQMomWebService {
  override def actorRefFactory: ActorRefFactory = system

  "RemoteHornetQMom" should "create/delete the given queue, send/receive message, get queues" in {

    Put(s"/createQueue/firstQueue") ~> routes ~> check {
      assertResult(Created)(status)
    }

    Put(s"/sendMessage/hello/firstQueue") ~> routes ~> check {
      assertResult(Accepted)(status)
    }

    Get(s"/getQueues") ~> routes ~> check {
      assertResult(OK)(status)
    }

    Put(s"/deleteQueue/firstQueue") ~> routes ~> check {
      assertResult(OK)(status)
    }
  }

  "RemoteHornetQMom" should "receive a message from the given queue with the given timeout and return an OK" in {
    Get(s"/receiveMessage/firstQueue/2") ~> routes ~> check {
            println("!!!!!"+new String(body.data.toByteArray))
      assertResult(OK)(status)
    }
    // default 20sec
//    Get(s"/receiveMessage/firstQueue") ~> routes ~> check {
//      assertResult(OK)(status)
//    }
//    // 0sec for an immediate return
//    Get(s"/receiveMessage/firstQueue?timeOutDuration=0sec") ~> routes ~> check {
//      assertResult(OK)(status)
//    }
  }

  "RemoteHornetQMom" should "acknowledge and return success code with no content" in {

//    Put(s"/acknowledge?message=messageReceived") ~> routes ~> check {
//      assertResult(NoContent)(status)
//    }
  }

}