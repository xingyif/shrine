package net.shrine.dashboard

import java.security.PrivateKey
import javax.crypto.SecretKey

import io.jsonwebtoken.{SignatureAlgorithm, Jwts}
import io.jsonwebtoken.impl.crypto.MacProvider
import net.shrine.authorization.steward.OutboundUser
import net.shrine.crypto.{KeyStoreDescriptorParser, KeyStoreCertCollection}
import net.shrine.dashboard.jwtauth.ShrineJwtAuthenticator
import net.shrine.i2b2.protocol.pm.User
import net.shrine.protocol.Credential
import org.json4s.native.JsonMethods.parse
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FlatSpec
import spray.http.HttpHeaders.Authorization
import spray.http.{HttpHeader, BasicHttpCredentials}

import spray.testkit.ScalatestRouteTest
import spray.http.StatusCodes.{OK,PermanentRedirect,Unauthorized}

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

  "DashboardService" should  "redirect several urls to client/index.html" in {

    Get() ~>
      route ~> check {
      status === PermanentRedirect
      header("Location") === "client/index.html"
    }

    Get("/") ~>
      route ~> check {
      status === PermanentRedirect
      header("Location") === "client/index.html"
    }

    Get("/index.html") ~>
      route ~> check {

      status === PermanentRedirect
      header("Location") === "client/index.html"
    }

    Get("/client") ~>
      route ~> check {

      status === PermanentRedirect
      header("Location") === "client/index.html"
    }

    Get("/client/") ~>
      route ~> check {

      status === PermanentRedirect
      header("Location") === "client/index.html"
    }

  }

  "DashboardService" should  "return an OK and the right version string for an admin/happy/version test" in {

    Get(s"/admin/happy/version") ~>
      addCredentials(adminCredentials) ~>
      route ~> check {

      assertResult(OK)(status)

      val versionString = new String(body.data.toByteArray)
      //todo test it to see if it's right
      println(versionString)
    }
  }

  "DashboardService" should  "return an OK and mess with the right version string for an admin/messWithHappyVersion test" in {

    Get(s"/admin/messWithHappyVersion") ~>
      addCredentials(adminCredentials) ~>
      route ~> check {

      assertResult(OK)(status)

      val versionString = new String(body.data.toByteArray)
      //todo test it to see if it's right
      println(versionString)
    }
  }

  val dashboardCredentials = BasicHttpCredentials(adminUserName,"shh!")

  "DashboardService" should  "return an OK and pong for remoteDashboard/ping" in {

    //todo how to generate one from a cert? Maybe?
    val config = DashboardConfigSource.config
    val shrineCertCollection: KeyStoreCertCollection = KeyStoreCertCollection.fromClassPathResource(KeyStoreDescriptorParser(config.getConfig("shrine.keystore")))

    val signerSerialNumber = shrineCertCollection.myCertId.get.serial
    val key: PrivateKey = shrineCertCollection.myKeyPair.privateKey
    val jwtsString = Jwts.builder().setSubject("Joe").signWith(SignatureAlgorithm.RS512, key).compact()

    Get(s"/remoteDashboard/ping") ~>
      addHeader(Authorization.name,s"${ShrineJwtAuthenticator.ShrineJwtAuth0}: $signerSerialNumber: $jwtsString") ~>
      route ~> check {

      assertResult(OK)(status)

      val string = new String(body.data.toByteArray)
      //todo test it to see if it's right
      assertResult(""""pong"""")(string)
    }
  }

  "DashboardService" should  "reject a remoteDashboard/ping with no Authorization header" in {

    Get(s"/remoteDashboard/ping") ~>
      sealRoute(route) ~> check {

      assertResult(Unauthorized)(status)
    }
  }

  "DashboardService" should  "reject a remoteDashboard/ping with no Authorization header for the wrong authorization spec" in {

    Get(s"/remoteDashboard/ping") ~>
      addCredentials(adminCredentials) ~>
      sealRoute(route) ~> check {

      assertResult(Unauthorized)(status)
    }
  }

}

