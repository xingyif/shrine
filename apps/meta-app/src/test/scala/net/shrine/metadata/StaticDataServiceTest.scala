package net.shrine.metadata

import akka.actor.ActorRefFactory
import org.json4s.DefaultFormats
import org.json4s.native.JsonMethods.parse
import org.junit.runner.RunWith
import org.scalatest.FlatSpec
import org.scalatest.junit.JUnitRunner
import spray.testkit.ScalatestRouteTest

/**
  * @author david 
  * @since 3/30/17
  */
@RunWith(classOf[JUnitRunner])
class StaticDataServiceTest extends FlatSpec with ScalatestRouteTest with StaticDataService {
  override def actorRefFactory: ActorRefFactory = system

  import scala.concurrent.duration._
  implicit val routeTestTimeout = RouteTestTimeout(10.seconds)
  import spray.http.StatusCodes._

  "StaticDataService" should "return an OK and 10 for a nested data" in {
    Get(s"/staticData?key=object.objectVal") ~> staticDataRoute ~> check {
      implicit val formats = DefaultFormats
      val result = Integer.valueOf(body.data.asString)

      assertResult(OK)(status)
      assertResult(result)(10)
    }
  }

  "StaticDataService" should "return an OK for all data" in {
    Get(s"/staticData") ~> staticDataRoute ~> check {
      implicit val formats = DefaultFormats
      val result = parse(new String(body.data.toByteArray))

      assertResult(OK)(status)
    }
  }

  "StaticDataService" should "return an Ok and a list for a data" in {
    Get("/staticData?key=list") ~> staticDataRoute ~> check {
      implicit val formats = DefaultFormats
      val result = new String(body.data.toByteArray)

      assertResult(OK)(status)
      assertResult(parse(result).extract[List[String]])(Seq("list", "list", "list"))

    }
  }

  "StaticDataService" should "return an OK and 10 for a nested data using the old data path" in {
    Get(s"/data?key=object.objectVal") ~> staticDataRoute ~> check {
      implicit val formats = DefaultFormats
      val result = Integer.valueOf(body.data.asString)

      assertResult(OK)(status)
      assertResult(result)(10)
    }
  }

  "StaticDataService" should "return an OK for all data using the old data path" in {
    Get(s"/data") ~> staticDataRoute ~> check {
      implicit val formats = DefaultFormats
      val result = parse(new String(body.data.toByteArray))

      assertResult(OK)(status)
    }
  }

  "StaticDataService" should "return an Ok and a list for a data using the old data path" in {
    Get("/data?key=list") ~> staticDataRoute ~> check {
      implicit val formats = DefaultFormats
      val result = new String(body.data.toByteArray)

      assertResult(OK)(status)
      assertResult(parse(result).extract[List[String]])(Seq("list", "list", "list"))

    }
  }
}