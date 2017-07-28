import org.json4s.DefaultFormats
import org.junit.runner.RunWith
import org.scalatest.FlatSpec
import org.scalatest.junit.JUnitRunner
import spray.testkit.ScalatestRouteTest

/**
  * Created by yifan on 7/27/17.
  */


@RunWith(classOf[JUnitRunner])
class RemoteHornetQMomTest extends FlatSpec with ScalatestRouteTest with RemoteHornetQMom {
//  import scala.concurrent.duration._
//  implicit val routeTestTimeout = RouteTestTimeout(10.seconds)
  import spray.http.StatusCodes._



  "RemoteHornetQMom" should "return an OK and a Queue" in {

    Get(s"/createQueue?queueName=firstQueue") ~> createQueue ~> check {
//      implicit val formats = DefaultFormats
//      val result = body.data.asString

      assertResult(OK)(status)
//      assertResult(result)(10)
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

}