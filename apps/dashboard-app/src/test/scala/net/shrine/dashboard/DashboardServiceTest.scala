package net.shrine.dashboard

import net.shrine.authorization.steward.OutboundUser
import net.shrine.i2b2.protocol.pm.User
import net.shrine.protocol.Credential
import org.json4s.native.JsonMethods.parse
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FlatSpec
import spray.http.BasicHttpCredentials

import spray.testkit.ScalatestRouteTest
import spray.http.StatusCodes.OK

@RunWith(classOf[JUnitRunner])
class DashboardServiceTest extends FlatSpec with ScalatestRouteTest with DashboardService {
  def actorRefFactory = system

  import scala.concurrent.duration._
  implicit val routeTestTimeout = RouteTestTimeout(10 seconds)

  val adminUserName = "keith"
  val adminFullName = adminUserName

  /**
   * to run these tests with I2B2
   * add a user named keith, to be the admin
   * add a Boolean parameter for keith, Admin, true
   * add all this user to the i2b2 project
   */
  val adminCredentials = BasicHttpCredentials(adminUserName,"shh!")

  val brokenCredentials = BasicHttpCredentials(adminUserName,"wrong password")

  val adminUser = User(
    fullName = adminUserName,
    username = adminFullName,
    domain = "domain",
    credential = new Credential("admin's password",false),
    params = Map(),
    rolesByProject = Map()
  )

  val adminOutboundUser = OutboundUser.createFromUser(adminUser)

  "DashboardService" should  "return an OK and a valid outbound user for a user/whoami request" in {

      Get(s"/user/whoami") ~>
        addCredentials(adminCredentials) ~>
        route ~> check {

        assertResult(OK)(status)

        val userJson = new String(body.data.toByteArray)
        val outboundUser = parse(userJson).extract[OutboundUser]
        assertResult(adminOutboundUser)(outboundUser)
      }
    }

  "DashboardService" should  "return an OK and a valid outbound user for a user/whoami request and an '' " in {

    Get(s"/user/whoami") ~>
      addCredentials(brokenCredentials) ~>
      route ~> check {

      assertResult(OK)(status)

      val response = new String(body.data.toByteArray)
      assertResult(""""AuthenticationFailed"""")(response)
    }
  }
}

