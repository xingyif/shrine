import akka.actor.ActorRefFactory
import net.shrine.metadata.RemoteHornetQMom
import net.shrine.mom.Queue
import org.json4s.DefaultFormats
import org.json4s.native.JsonMethods.parse
import org.junit.runner.RunWith
import org.scalatest.FlatSpec
import org.scalatest.junit.JUnitRunner
import spray.testkit.ScalatestRouteTest

/**
  * Created by yifan on 7/27/17.
  */


@RunWith(classOf[JUnitRunner])
class RemoteHornetQMomTest extends FlatSpec with ScalatestRouteTest with RemoteHornetQMom {
  override def actorRefFactory: ActorRefFactory = system
//
//  import scala.concurrent.duration._
//  implicit val routeTestTimeout = RouteTestTimeout(10.seconds)
  import spray.http.StatusCodes._


  "RemoteHornetQMom" should "return an OK and a Queue" in {

    Get(s"/createQueue?queueName=firstQueue") ~> createQueue ~> check {
      implicit val formats = DefaultFormats
      val result = body.data.asString
      val q: Queue = Queue(result)

      assertResult(OK)(status)
      assertResult(q.name)("firstQueue")
    }
  }

  "RemoteHornetQMom" should "return an OK and a delete Queue notification" in {

    Get(s"/deleteQueue?queueName=firstQueue") ~> deleteQueue ~> check {
      //      implicit val formats = DefaultFormats
      val result = body.data.asString
      val string = s"Queue 'firstQueue' deleted!"
      assertResult(OK)(status)
      assertResult(result)(string)
    }
  }

//  lazy val deleteQueue: Route = pathPrefix("deleteQueue") {
//    parameter('queueName) { (queueName: String) =>
//      deleteQueue(queueName)
//      complete(s"Queue '$queueName' deleted!") // todo
//    }
//  }
//
//  override def deleteQueue(queueName: String): (StatusCode, Unit) = {
//    // todo SQS takes in DeleteMessageRequest, which contains a queueUrl: String and a ReceiptHandle: String
//    // returns a DeleteMessageResult, toString for debugging
//    Try(StatusCodes.OK -> LocalHornetQMom.deleteQueue(queueName))
//      .getOrElse(StatusCodes.BadRequest
//        -> s"Given QueueName doesn't exist! Failed to delete!")
//  }




}