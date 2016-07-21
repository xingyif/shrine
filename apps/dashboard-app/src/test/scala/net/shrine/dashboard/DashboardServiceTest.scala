package net.shrine.dashboard

import java.security.PrivateKey
import java.util.Date

import io.jsonwebtoken.impl.TextCodec
import io.jsonwebtoken.{Jwts, SignatureAlgorithm}
import net.shrine.authorization.steward.OutboundUser
import net.shrine.crypto.{KeyStoreCertCollection, KeyStoreDescriptorParser}
import net.shrine.dashboard.jwtauth.ShrineJwtAuthenticator
import net.shrine.i2b2.protocol.pm.User
import net.shrine.protocol.Credential
import org.json4s.native.JsonMethods.parse
import org.junit.runner.RunWith
import org.scalatest.FlatSpec
import org.scalatest.junit.JUnitRunner
import spray.http.StatusCodes.{OK, PermanentRedirect, Unauthorized}
import spray.http.{BasicHttpCredentials, OAuth2BearerToken}
import spray.testkit.ScalatestRouteTest

import scala.language.postfixOps

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

  "DashboardService" should  "return an OK and the right version string for an admin/happy/all test" in {

    Get(s"/admin/happy/all") ~>
      addCredentials(adminCredentials) ~>
      route ~> check {

      assertResult(OK)(status)

      val allString = new String(body.data.toByteArray)
      //println(allString)
      //todo test it to see if it's right
    }
  }

  "DashboardService" should  "return an OK and mess with the right version string for an admin/messWithHappyVersion test" in {

    Get(s"/admin/messWithHappyVersion") ~>
      addCredentials(adminCredentials) ~>
      route ~> check {

      assertResult(OK)(status)

      val versionString = new String(body.data.toByteArray)
      //todo test it to see if it's right
    }
  }

  "DashboardService" should  "return an OK for admin/status/config" in {

    Get(s"/admin/status/config") ~>
      addCredentials(adminCredentials) ~>
      route ~> check {

        assertResult(OK)(status)

        val configString = new String(body.data.toByteArray)
        //println(configString)
      }
  }

  "DashboardService" should  "return an OK for admin/status/classpath" in {

    Get(s"/admin/status/classpath") ~>
      addCredentials(adminCredentials) ~>
      route ~> check {

      assertResult(OK)(status)

      val classpathString = new String(body.data.toByteArray)
    }
  }

  "DashboardService" should  "return an OK for admin/status/options" in {

    Get(s"/admin/status/options") ~>
      addCredentials(adminCredentials) ~>
      route ~> check {

      assertResult(OK)(status)

      val options = new String(body.data.toByteArray)
    }
  }

  "DashboardService" should  "return an OK for admin/status/summary" in {

    Get(s"/admin/status/summary") ~>
      addCredentials(adminCredentials) ~>
      route ~> check {

      assertResult(OK)(status)

      val summary = new String(body.data.toByteArray)
    }
  }


  "DashboardService" should "return an OK for admin/status/problems" in {

    Get("/admin/status/problems") ~>
      addCredentials(adminCredentials) ~>
        route ~> check {
        assertResult(OK)(status)

        val problems = new String(body.data.toByteArray)
      }
  }

  "DashboardService" should "return an OK for admin/status/problems with queries" in {

    Get("/admin/status/problems?offset=5") ~>
      addCredentials(adminCredentials) ~>
      route ~> check {
      assertResult(OK)(status)

      val problems = new String(body.data.toByteArray)
    }
  }

  val dashboardCredentials = BasicHttpCredentials(adminUserName,"shh!")

  "DashboardService" should  "return an OK and pong for fromDashboard/ping" in {

    Get(s"/fromDashboard/ping") ~>
      addCredentials(ShrineJwtAuthenticator.createOAuthCredentials(adminUser)) ~>
      route ~> check {

      assertResult(OK)(status)

      val string = new String(body.data.toByteArray)

      assertResult(""""pong"""")(string)
    }
  }

  "DashboardService" should  "reject a fromDashboard/ping with an expired jwts header" in {

    val config = DashboardConfigSource.config
    val shrineCertCollection: KeyStoreCertCollection = KeyStoreCertCollection.fromFileRecoverWithClassPath(KeyStoreDescriptorParser(config.getConfig("shrine.keystore")))

    val base64Cert = new String(TextCodec.BASE64URL.encode(shrineCertCollection.myCert.get.getEncoded))

    val key: PrivateKey = shrineCertCollection.myKeyPair.privateKey
    val expiration: Date = new Date(System.currentTimeMillis() - 300 * 1000) //bad for 5 minutes
    val jwtsString = Jwts.builder().
        setHeaderParam("kid", base64Cert).
        setSubject(java.net.InetAddress.getLocalHost.getHostName).
        setExpiration(expiration).
        signWith(SignatureAlgorithm.RS512, key).
        compact()

    Get(s"/fromDashboard/ping") ~>
      addCredentials(OAuth2BearerToken(jwtsString)) ~>
      sealRoute(route) ~> check {

      assertResult(Unauthorized)(status)
    }
  }

  "DashboardService" should  "reject a fromDashboard/ping with no subject" in {

    val config = DashboardConfigSource.config
    val shrineCertCollection: KeyStoreCertCollection = KeyStoreCertCollection.fromClassPathResource(KeyStoreDescriptorParser(config.getConfig("shrine.keystore")))

    val base64Cert = new String(TextCodec.BASE64URL.encode(shrineCertCollection.myCert.get.getEncoded))

    val key: PrivateKey = shrineCertCollection.myKeyPair.privateKey
    val expiration: Date = new Date(System.currentTimeMillis() + 30 * 1000)
    val jwtsString = Jwts.builder().
        setHeaderParam("kid", base64Cert).
        setExpiration(expiration).
        signWith(SignatureAlgorithm.RS512, key).
        compact()

    Get(s"/fromDashboard/ping") ~>
      addCredentials(OAuth2BearerToken(jwtsString)) ~>
      sealRoute(route) ~> check {

      assertResult(Unauthorized)(status)
    }
  }

  "DashboardService" should  "reject a fromDashboard/ping with no Authorization header" in {

    Get(s"/fromDashboard/ping") ~>
      sealRoute(route) ~> check {

      assertResult(Unauthorized)(status)
    }
  }

  "DashboardService" should  "reject a fromDashboard/ping with an Authorization header for the wrong authorization spec" in {

    Get(s"/fromDashboard/ping") ~>
      addCredentials(adminCredentials) ~>
      sealRoute(route) ~> check {

      assertResult(Unauthorized)(status)
    }
  }

  /*
  "DashboardService" should  "not find a bogus web service to talk to" in {

    Get(s"/toDashboard/bogus.harvard.edu/ping") ~>
      addCredentials(adminCredentials) ~>
      sealRoute(route) ~> check {

      val string = new String(body.data.toByteArray)

      assertResult(NotFound)(status)
    }
  }
*/

}

