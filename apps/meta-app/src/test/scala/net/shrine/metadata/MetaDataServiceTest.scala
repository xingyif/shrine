package net.shrine.metadata

import akka.actor.ActorRefFactory
import org.json4s.DefaultFormats
import org.json4s.native.JsonMethods.parse
import org.junit.runner.RunWith
import org.scalatest.FlatSpec
import org.scalatest.junit.JUnitRunner
import spray.testkit.ScalatestRouteTest

@RunWith(classOf[JUnitRunner])
class MetaDataServiceTest extends FlatSpec with ScalatestRouteTest with MetaDataService {
  override def actorRefFactory: ActorRefFactory = system

  import scala.concurrent.duration._
  implicit val routeTestTimeout = RouteTestTimeout(10.seconds)
  import spray.http.StatusCodes._


  "MetaDataService" should "return an OK and pong for a ping" in {
    Get(s"/ping") ~> route ~> check {
      implicit val formats = DefaultFormats
      val result = new String(body.data.toByteArray)

      assertResult(OK)(status)
      assertResult(result)("pong")
    }
  }

  "MetaDataService" should "return an OK and pong for a data" in {
    Get(s"/data?key=ping") ~> route ~> check {
      implicit val formats = DefaultFormats
      val result = new String(body.data.toByteArray)

      assertResult(OK)(status)
      assertResult(result)("\"pong\"")
    }
  }

  "MetaDataService" should "return an OK and 10 for a nested data" in {
    Get(s"/data?key=object.objectVal") ~> route ~> check {
      implicit val formats = DefaultFormats
      val result = Integer.valueOf(body.data.asString)

      assertResult(OK)(status)
      assertResult(result)(10)
    }
  }

  "MetaDataService" should "return an OK for all data" in {
    Get(s"/data") ~> route ~> check {
      implicit val formats = DefaultFormats
      val result = parse(new String(body.data.toByteArray))

      assertResult(OK)(status)
    }
  }

  "MetaDataService" should "return an Ok and a list for a data" in {
    Get("/data?key=list") ~> route ~> check {
      implicit val formats = DefaultFormats
      val result = new String(body.data.toByteArray)

      assertResult(OK)(status)
      assertResult(parse(result).extract[List[String]])(Seq("list", "list", "list"))

    }
  }
}
