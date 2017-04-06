package net.shrine.dashboard

import java.security.PrivateKey
import java.util.Date

import io.jsonwebtoken.impl.TextCodec
import io.jsonwebtoken.{Jwts, SignatureAlgorithm}
import net.shrine.authorization.steward.OutboundUser
import net.shrine.config.ConfigExtensions
import net.shrine.crypto.{BouncyKeyStoreCollection, KeyStoreDescriptorParser}
import net.shrine.dashboard.jwtauth.ShrineJwtAuthenticator
import net.shrine.i2b2.protocol.pm.User
import net.shrine.protocol.Credential
import net.shrine.source.ConfigSource
import net.shrine.spray.ShaResponse
import org.json4s.native.JsonMethods.parse
import org.junit.runner.RunWith
import org.scalatest.FlatSpec
import org.scalatest.junit.JUnitRunner
import spray.http.StatusCodes.{NotFound, OK, PermanentRedirect, Unauthorized}
import spray.http.{BasicHttpCredentials, FormData, OAuth2BearerToken, StatusCodes}
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

        implicit val formats = OutboundUser.json4sFormats
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
      assertResult("""AuthenticationFailed""")(response)
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

  "DashboardService" should  "return an OK and the right version string for an admin/happy/all?extra=true test" in {

    Get(s"/admin/happy/all?extra=true") ~>
      addCredentials(adminCredentials) ~>
      route ~> check {

      assertResult(OK)(status)

      val allString = new String(body.data.toByteArray)
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

  "DashboardService" should  "return an OK for admin/status/optionalParts" in {

    Get(s"/admin/status/optionalParts") ~>
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

  "DashboardService" should  "return an OK for admin/status/adapter" in {

    Get(s"/admin/status/adapter") ~>
      addCredentials(adminCredentials) ~>
      route ~> check {

      assertResult(OK)(status)

      val adapter = new String(body.data.toByteArray)
    }
  }

  "DashboardService" should  "return an OK for admin/status/hub" in {

    Get(s"/admin/status/hub") ~>
      addCredentials(adminCredentials) ~>
      route ~> check {

      assertResult(OK)(status)

      val hub = new String(body.data.toByteArray)
    }
  }

  "DashboardService" should  "return an OK for admin/status/i2b2" in {

    Get(s"/admin/status/i2b2") ~>
      addCredentials(adminCredentials) ~>
      route ~> check {

      assertResult(OK)(status)

      val i2b2 = new String(body.data.toByteArray)
    }
  }

  "DashboardService" should  "return an OK for admin/status/keystore" in {

    Get(s"/admin/status/keystore") ~>
      addCredentials(adminCredentials) ~>
      route ~> check {

      assertResult(OK)(status)

      val keystore = new String(body.data.toByteArray)
    }
  }

  "DashboardService" should  "return an OK for admin/status/qep" in {

    Get(s"/admin/status/qep") ~>
      addCredentials(adminCredentials) ~>
      route ~> check {

      assertResult(OK)(status)

      val qep = new String(body.data.toByteArray)
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

    Get("/admin/status/problems?offset=2&n=1") ~>
      addCredentials(adminCredentials) ~>
      route ~> check {
        assertResult(OK)(status)

        val problems = new String(body.data.toByteArray)
      }
  }

  "DashboardService" should "return an OK for admin/status/problems with queries and an epoch filter" in {

    Get("/admin/status/problems?offset=2&n=3&epoch=3") ~>
      addCredentials(adminCredentials) ~>
      route ~> check {
        assertResult(OK)(status)

        val problems = new String(body.data.toByteArray)
      }
  }

  "DashboardService" should "return a BadRequest for admin/status/signature with no signature parameter" in {
    Post("/status/verifySignature") ~>
    addCredentials(adminCredentials) ~>
    route ~> check {
      assertResult(StatusCodes.BadRequest)(status)
    }
  }

  "DashboardService" should "return a BadRequest for admin/status/signature with a malformatted signature parameter" in {
    Post("/status/verifySignature", FormData(Seq("sha256" -> "foo"))) ~>
      addCredentials(adminCredentials) ~>
      route ~> check {
      assertResult(StatusCodes.BadRequest)(status)
      implicit val formats = ShaResponse.json4sFormats
      assertResult(ShaResponse(ShaResponse.badFormat, false))(parse(new String(body.data.toByteArray)).extract[ShaResponse])
    }
  }

  "DashboardService" should "return a NotFound for admin/status/signature with a correctly formatted parameter that is not in the keystore" in {
    Post("/status/verifySignature", FormData(Seq("sha256" -> "00:00:00:00:00:00:00:7C:4B:FD:8D:A8:0A:C7:A4:AA:13:3E:22:B3:57:A7:C6:B0:95:15:1B:22:C0:E5:15:9A"))) ~>
      addCredentials(adminCredentials) ~>
      route ~> check {
      assertResult(NotFound)(status)
      implicit val formats = ShaResponse.json4sFormats
      assertResult(ShaResponse("0E:5D:D1:10:68:2B:63:F4:66:E2:50:41:EA:13:AF:1A:F9:99:DB:40:6A:F7:EE:39:F2:1A:0D:51:7A:44:09:7A", false)) (
        parse(new String(body.data.toByteArray)).extract[ShaResponse])
    }
  }

  "DashboardService" should "return an OK for admin/status/signature with a valid sha256 hash" in {
    val post = Post("/status/verifySignature", FormData(Seq("sha256" -> "0E:5D:D1:10:68:2B:63:F4:66:E2:50:41:EA:13:AF:1A:F9:99:DB:40:6A:F7:EE:39:F2:1A:0D:51:7A:44:09:7A")))
      post ~>
      addCredentials(adminCredentials) ~>
      route ~> check {
      assertResult(OK)(status)
      implicit val formats = ShaResponse.json4sFormats
      assertResult(ShaResponse("0E:5D:D1:10:68:2B:63:F4:66:E2:50:41:EA:13:AF:1A:F9:99:DB:40:6A:F7:EE:39:F2:1A:0D:51:7A:44:09:7A", true))(
        parse(new String(body.data.toByteArray)).extract[ShaResponse]
      )
    }
  }


  val dashboardCredentials = BasicHttpCredentials(adminUserName,"shh!")

  "DashboardService" should  "return an OK and pong for fromDashboard/ping" in {

    Get(s"/fromDashboard/ping") ~>
      addCredentials(ShrineJwtAuthenticator.createOAuthCredentials(adminUser, "")) ~>
      route ~> check {

      assertResult(OK)(status)

      val string = new String(body.data.toByteArray)

      assertResult("pong")(string)
    }
  }

  "DashboardService" should  "reject a fromDashboard/ping with an expired jwts header" in {

    val config = ConfigSource.config
    val shrineCertCollection: BouncyKeyStoreCollection = BouncyKeyStoreCollection.fromFileRecoverWithClassPath(KeyStoreDescriptorParser(
      config.getConfig("shrine.keystore"),
      config.getConfigOrEmpty("shrine.hub"),
      config.getConfigOrEmpty("shrine.queryEntryPoint")))

    val base64Cert = new String(TextCodec.BASE64URL.encode(shrineCertCollection.myEntry.cert.getEncoded))

    val key: PrivateKey = shrineCertCollection.myEntry.privateKey.get
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

    val config = ConfigSource.config
    val shrineCertCollection: BouncyKeyStoreCollection = BouncyKeyStoreCollection.fromFileRecoverWithClassPath(KeyStoreDescriptorParser(
      config.getConfig("shrine.keystore"),
      config.getConfigOrEmpty("shrine.hub"),
      config.getConfigOrEmpty("shrine.queryEntryPoint")))

    val base64Cert = new String(TextCodec.BASE64URL.encode(shrineCertCollection.myEntry.cert.getEncoded))

    val key: PrivateKey = shrineCertCollection.myEntry.privateKey.get
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


  "DashboardService" should "not be able to make a toDashboard request" in {
    // Can't make a request because it's configured as a downstream node
    Get(s"/toDashboard/bogus.harvard.edu/ping") ~>
      addCredentials(adminCredentials) ~>
      sealRoute(route) ~> check {

      val string = new String(body.data.toByteArray)

      assertResult(StatusCodes.Forbidden)(status)
    }
  }

}

