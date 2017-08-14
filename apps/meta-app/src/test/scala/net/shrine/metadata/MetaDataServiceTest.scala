package net.shrine.metadata

import akka.actor.ActorRefFactory
import net.shrine.i2b2.protocol.pm.User
import net.shrine.protocol.Credential
import org.json4s.DefaultFormats
import org.json4s.native.JsonMethods.parse
import org.junit.runner.RunWith
import org.scalatest.FlatSpec
import org.scalatest.junit.JUnitRunner
import spray.http.BasicHttpCredentials
import spray.testkit.ScalatestRouteTest

import scala.concurrent.ExecutionContext

@RunWith(classOf[JUnitRunner])
class MetaDataServiceTest extends FlatSpec with ScalatestRouteTest with MetaDataService {
  override def actorRefFactory: ActorRefFactory = system
  override implicit val ec: ExecutionContext = ExecutionContext.Implicits.global

  import scala.concurrent.duration._
  implicit val routeTestTimeout = RouteTestTimeout(10.seconds)
  import spray.http.StatusCodes._

  "MetaDataService" should "return an OK and pong for a ping" in {
    Get(s"/ping") ~> route ~> check {
      implicit val formats = DefaultFormats
      val result = new String(body.data.toByteArray)

      assertResult(OK)(status)
      assertResult("pongpongpong...!")(result)
    }
  }

  "MetaDataService" should "access the staticData service and return an OK and 10 for a nested data" in {
    Get(s"/staticData?key=object.objectVal") ~> route ~> check {
      implicit val formats = DefaultFormats
      val result = Integer.valueOf(body.data.asString)

      assertResult(OK)(status)
      assertResult(10)(result)
    }
  }

  val researcherUserName = "ben"
  val researcherFullName = researcherUserName

  val researcherCredentials = BasicHttpCredentials(researcherUserName,"kapow")
  val brokenCredentials = BasicHttpCredentials(researcherUserName,"wrong password")

  "MetaDataService" should "access the qep service and return an OK for its instructions after a login" in {
    Get(s"/qep") ~>  addCredentials(researcherCredentials) ~> route ~>
    check {
      assertResult(OK)(status)
    }
  }

  "MetaDataService" should "not access the qep service without a username and password" in {
    Get(s"/qep") ~>  sealRoute(route) ~>
      check { assertResult(Unauthorized)(status) }
  }

  "MetaDataService" should "not access the qep service with an incorrect password" in {
    Get(s"/qep") ~>  addCredentials(brokenCredentials) ~>  sealRoute(route) ~>
      check { assertResult(Unauthorized)(status) }
  }
}
