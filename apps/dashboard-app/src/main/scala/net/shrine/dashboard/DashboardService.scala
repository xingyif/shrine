package net.shrine.dashboard

import akka.actor.Actor
import akka.event.Logging
import net.shrine.authentication.UserAuthenticator
import net.shrine.authorization.steward.OutboundUser
import net.shrine.config.ConfigExtensions
import net.shrine.crypto.{BouncyKeyStoreCollection, KeyStoreDescriptorParser, UtilHasher}
import net.shrine.dashboard.httpclient.HttpClientDirectives.{forwardUnmatchedPath, requestUriThenRoute}
import net.shrine.dashboard.jwtauth.ShrineJwtAuthenticator
import net.shrine.i2b2.protocol.pm.User
import net.shrine.log.Loggable
import net.shrine.problem.{AbstractProblem, ProblemDigest, ProblemSources, Problems}
import net.shrine.serialization.NodeSeqSerializer
import net.shrine.source.ConfigSource
import net.shrine.spray._
import net.shrine.status.protocol.{Config => StatusProtocolConfig}
import net.shrine.util.{SingleHubModel, Versions}
import org.json4s.native.JsonMethods.{parse => json4sParse}
import org.json4s.{DefaultFormats, Formats}
import shapeless.HNil
import spray.http._
import spray.httpx.Json4sSupport
import spray.routing._
import spray.routing.directives.LogEntry

import scala.collection.immutable.Iterable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success, Try}

/**
  * Mixes the DashboardService trait with an Akka Actor to provide the actual service.
  */
class DashboardServiceActor extends Actor with DashboardService {

  // the HttpService trait defines only one abstract member, which
  // connects the services environment to the enclosing actor or test
  def actorRefFactory = context

  // this actor only runs our route, but you could add
  // other things here, like request stream processing
  // or timeout handling
  def receive = runRoute(route)
}

/**
  * A web service that provides the Dashboard endpoints. It is a trait to support testing independent of Akka.
  */

trait DashboardService extends HttpService with Loggable {

  val userAuthenticator = UserAuthenticator(ConfigSource.config)

  //don't need to do anything special for unauthorized users, but they do need access to a static form.
  lazy val route: Route = gruntWatchCorsSupport {
    redirectToIndex ~ staticResources ~ versionCheck ~ makeTrouble ~ about ~ authenticatedInBrowser ~ authenticatedDashboard ~ post {
      // Chicken and egg problem; Can't check status of certs validation between sites if you need valid certs to exchange messages
      pathPrefix("status")
      pathPrefix("verifySignature")
      verifySignature
    }
  }

  /** logs the request method, uri and response at info level */
  def logEntryForRequestResponse(req: HttpRequest): Any => Option[LogEntry] = {
    case res: HttpResponse => Some(LogEntry(s"\n  Request: $req\n  Response: $res", Logging.InfoLevel))
    case _ => None // other kind of responses
  }

  /** logs just the request method, uri and response status at info level */
  def logEntryForRequest(req: HttpRequest): Any => Option[LogEntry] = {
    case res: HttpResponse => Some(LogEntry(s"\n  Request: $req\n  Response status: ${res.status}", Logging.InfoLevel))
    case _ => None // other kind of responses
  }

  lazy val versionCheck = pathPrefix("version") {

    val currentVersion = Versions.version
    val buildDate = Versions.buildDate

    val response: AppVersion = AppVersion(currentVersion, buildDate)
    implicit val formats = response.json4sMarshaller
    complete(response)
  }

  case class AppVersion(currentVersion: String, buildDate: String) extends DefaultJsonSupport {

    override def toString: String = {
      "{\"currentVersion\":" + "\"" + currentVersion + "\"" + "," + "\"buildDate\":\"" + buildDate + "\"}"
    }

  }

  def authenticatedInBrowser: Route = pathPrefixTest("user" | "admin" | "toDashboard") {
    logRequestResponse(logEntryForRequestResponse _) { //logging is controlled by Akka's config, slf4j, and log4j config
      reportIfFailedToAuthenticate {
        authenticate(userAuthenticator.basicUserAuthenticator) { user =>
          pathPrefix("user") {
            userRoute(user)
          } ~
            pathPrefix("admin") {
              adminRoute(user)
            } ~
            pathPrefix("toDashboard") {
              toDashboardRoute(user)
            }
        }
      }
    }
  }

  val reportIfFailedToAuthenticate = routeRouteResponse {
    case Rejected(List(AuthenticationFailedRejection(_, _))) =>
      complete("AuthenticationFailed")
  }

  def authenticatedDashboard: Route = pathPrefix("fromDashboard") {
    logRequestResponse(logEntryForRequestResponse _) { //logging is controlled by Akka's config, slf4j, and log4j config
      get { //all remote dashboard calls are gets.
        authenticate(ShrineJwtAuthenticator.authenticate) { user =>
          info(s"Sucessfully authenticated user `$user`")
          adminRoute(user)
        }
      }
    }
  }

  def makeTrouble = pathPrefix("makeTrouble") {
    complete(throw new IllegalStateException("fake trouble"))
  }

  lazy val redirectToIndex = pathEnd {
    redirect("shrine-dashboard/client/index.html", StatusCodes.PermanentRedirect) //todo pick up "shrine-dashboard" programatically
  } ~
    (path("index.html") | pathSingleSlash) {
      redirect("client/index.html", StatusCodes.PermanentRedirect)
    }

  lazy val staticResources = pathPrefix("client") {
    pathEnd {
      redirect("client/index.html", StatusCodes.PermanentRedirect)
    } ~
      pathSingleSlash {
        redirect("index.html", StatusCodes.PermanentRedirect)
      } ~ {
      getFromResourceDirectory("client")
    }
  }

  lazy val about = pathPrefix("about") {
    complete("Something is here already") //todo
  }

  def userRoute(user: User): Route = get {
    pathPrefix("whoami") {
      complete(OutboundUser.createFromUser(user))
    }
  }

  //todo check that this an admin.
  def adminRoute(user: User): Route = get {

    pathPrefix("happy") {
      val happyBaseUrl: String = ConfigSource.config.getString("shrine.dashboard.happyBaseUrl")

      forwardUnmatchedPath(happyBaseUrl)
    } ~
      pathPrefix("messWithHappyVersion") { //todo is this used?
        val happyBaseUrl: String = ConfigSource.config.getString("shrine.dashboard.happyBaseUrl")

        def pullClasspathFromConfig(httpResponse: HttpResponse, uri: Uri): Route = {
          ctx => {
            val result = httpResponse.entity.asString
            ctx.complete(s"Got '$result' from $uri")
          }
        }

        requestUriThenRoute(happyBaseUrl + "/version", pullClasspathFromConfig)
      } ~
      pathPrefix("ping") {
        complete("pong")
      } ~
      pathPrefix("status") {
        statusRoute(user)
      }
  }

  //Manually test this by running a curl command
  //curl -k -w "\n%{response_code}\n" -u dave:kablam "https://shrine-dev1.catalyst:6443/shrine-dashboard/toDashboard/shrine-dev2.catalyst/ping"
  /**
    * Forward a request from this dashboard to a remote dashboard
    */
  def toDashboardRoute(user: User): Route = get {

    pathPrefix(Segment) { dnsName =>
      import scala.collection.JavaConversions._

      // Check that it makes sense to call toDashboard
      KeyStoreInfo.keyStoreDescriptor.trustModel match {
        case SingleHubModel(false) =>
          warn("toDashboard route called on a non-hub node, returning Forbidden")
          complete(StatusCodes.Forbidden)
        case _ =>
          ConfigSource.config.getObject("shrine.hub.downstreamNodes")
            .values
            .map(cv => Try(new java.net.URL(cv.unwrapped().toString)) match {
              case Failure(exception) =>
                MalformedURLProblem(exception, cv.unwrapped().toString)
                throw exception
              case Success(goodUrl) => goodUrl
            })
            .find(_.getHost == dnsName) match {
            case None =>
              warn(s"Could not find a downstream node matching the requested host `$dnsName`, returning NotFound")
              complete(StatusCodes.NotFound)
            case Some(downstreamUrl) =>
              val remoteDashboardPathPrefix = downstreamUrl.getPath
                .replaceFirst("shrine/rest/adapter/requests", "shrine-dashboard/fromDashboard") // I don't think this needs to be configurable
            val port = if (downstreamUrl.getPort == -1)
              downstreamUrl.getDefaultPort
            else
              downstreamUrl.getPort

              val baseUrl = s"${downstreamUrl.getProtocol}://$dnsName:$port$remoteDashboardPathPrefix"

              info(s"toDashboardRoute: BaseURL: $baseUrl")
              forwardUnmatchedPath(baseUrl, Some(ShrineJwtAuthenticator.createOAuthCredentials(user, dnsName)))
          }
      }
    }
  }

  case class MalformedURLProblem(malformattedURLException: Throwable, malformattedURL: String) extends AbstractProblem(ProblemSources.Dashboard) {
    override val throwable = Some(malformattedURLException)

    override def summary: String = s"Encountered a malformatted url `$malformattedURL` while parsing urls from downstream nodes"

    override def description: String = description
  }

  def statusRoute(user: User): Route = get {
    val (adapter, hub, i2b2, keystore, optionalParts, qep, summary) =
      ("adapter", "hub", "i2b2", "keystore", "optionalParts", "qep", "summary")
    pathPrefix("classpath") {
      getClasspath
    } ~
      pathPrefix("config") {
        getConfig
      } ~
      pathPrefix("problems") {
        getProblems
      } ~
      pathPrefix(adapter) {
        getFromSubService(adapter)
      } ~
      pathPrefix(hub) {
        getFromSubService(hub)
      } ~
      pathPrefix(i2b2) {
        getFromSubService(i2b2)
      } ~
      pathPrefix(keystore) {
        getFromSubService(keystore)
      } ~
      pathPrefix(optionalParts) {
        getFromSubService(optionalParts)
      } ~
      pathPrefix(qep) {
        getFromSubService(qep)
      } ~
      pathPrefix(summary) {
        getFromSubService(summary)
      }
  }

  val statusBaseUrl = ConfigSource.config.getString("shrine.dashboard.statusBaseUrl")

  // TODO: Move this over to Status API?
  lazy val verifySignature: Route = {

    formField("sha256".as[String].?) { sha256: Option[String] =>
      val response = sha256.map(s => KeyStoreInfo.hasher.handleSig(s))
      implicit val format = ShaResponse.json4sFormats
      response match {
        case None => complete(StatusCodes.BadRequest)
        case Some(sh@ShaResponse(ShaResponse.badFormat, _)) => complete(StatusCodes.BadRequest -> sh)
        case Some(sh@ShaResponse(_, false)) => complete(StatusCodes.NotFound -> sh)
        case Some(sh@ShaResponse(_, true)) => complete(StatusCodes.OK -> sh)
      }
    }
  }


  lazy val getConfig: Route = {

    def completeConfigRoute(httpResponse: HttpResponse, uri: Uri): Route = {
      ctx => {
        val config = ParsedConfig(httpResponse.entity.asString)
        ctx.complete(
          ShrineConfig(config)
        )
      }
    }

    requestUriThenRoute(statusBaseUrl + "/config", completeConfigRoute)
  }

  lazy val getClasspath: Route = {

    def pullClasspathFromConfig(httpResponse: HttpResponse, uri: Uri): Route = {
      ctx => {
        val result = httpResponse.entity.asString
        val shrineConfig = ShrineConfig(ParsedConfig(result))

        ctx.complete(shrineConfig)
      }
    }

    requestUriThenRoute(statusBaseUrl + "/config", pullClasspathFromConfig)
  }

  def getFromSubService(key: String): Route = {
    requestUriThenRoute(s"$statusBaseUrl/$key")
  }

  // table based view, can see N problems at a time. Front end sends how many problems that they want
  // to skip, and it will take N the 'nearest N' ie with n = 20, 0-19 -> 20, 20-39 -> 20-40
  lazy val getProblems: Route = {

    def floorMod(x: Int, y: Int) = {
      x - (x % y)
    }

    val db = Problems.DatabaseConnector

    // Intellij loudly complains if you use parameters instead of chained parameter calls.
    // ¯\_(ツ)_/¯
    parameter("offset".as[Int].?(0)) { (offsetPreMod: Int) =>
      parameter("n".as[Int].?(20)) { (nPreMax: Int) =>
        parameter("epoch".as[Long].?) { (epoch: Option[Long]) =>
          val n = Math.max(0, nPreMax)
          val moddedOffset = floorMod(Math.max(0, offsetPreMod), n)

          val query = for {
            result <- db.IO.sizeAndProblemDigest(n, moddedOffset)
          } yield (result._2, floorMod(Math.max(0, moddedOffset), n), n, result._1)

          val query2 = for {
            dateOffset <- db.IO.findIndexOfDate(epoch.getOrElse(0))
            moddedOffset = floorMod(dateOffset, n)
            result <- db.IO.sizeAndProblemDigest(n, moddedOffset)
          } yield (result._2, moddedOffset, n, result._1)

          val queryReal = if (epoch.isEmpty) query else query2
          val tupled = db.runBlocking(queryReal)
          val response: ProblemResponse = ProblemResponse(tupled._1, tupled._2, tupled._3, tupled._4)
          implicit val formats = response.json4sMarshaller
          complete(response)
        }
      }
    }
  }
}

case class ProblemResponse(size: Int, offset: Int, n: Int, problems: Seq[ProblemDigest]) extends Json4sSupport {
  override implicit def json4sFormats: Formats = DefaultFormats + new NodeSeqSerializer
}

object KeyStoreInfo {
  val config = ConfigSource.config
  val keyStoreDescriptor = KeyStoreDescriptorParser(
    config.getConfig("shrine.keystore"),
    config.getConfigOrEmpty("shrine.hub"),
    config.getConfigOrEmpty("shrine.queryEntryPoint"))
  val certCollection = BouncyKeyStoreCollection.fromFileRecoverWithClassPath(keyStoreDescriptor)
  val hasher = UtilHasher(certCollection)

}

/**
  * Centralized parsing logic for map of shrine.conf
  * the class literal `T.class` in Java.
  */
//todo most of this info should come directly from the status service in Shrine, not from reading the config
case class ParsedConfig(configMap: Map[String, String]) {

  private val trueVal = "true"
  private val rootKey = "shrine"

  def isHub =
    getOrElse(rootKey + ".hub.create", "")
      .toLowerCase == trueVal

  def stewardEnabled =
    configMap.keySet
      .contains(rootKey + ".queryEntryPoint.shrineSteward")

  def shouldQuerySelf =
    getOrElse(rootKey + ".hub.shouldQuerySelf", "")
      .toLowerCase == trueVal

  def fromJsonString(jsonString: String): String = jsonString.split("\"").mkString("")

  def get(key: String): Option[String] = configMap.get(key).map(fromJsonString)

  def getOrElse(key: String, elseVal: String = ""): String = get(key).getOrElse(elseVal)
}

object ParsedConfig {
  def apply(jsonString: String): ParsedConfig = {

    implicit def json4sFormats: Formats = DefaultFormats

    ParsedConfig(json4sParse(jsonString).extract[StatusProtocolConfig].keyValues) //.filterKeys(_.toLowerCase.startsWith("shrine")))
  }

}

case class DownstreamNode(name: String, url: String)

object DownstreamNode {
  def create(configMap: Map[String, String]): Iterable[DownstreamNode] = {
    for ((k, v) <- configMap.filterKeys(_.toLowerCase.startsWith
    ("shrine.hub.downstreamnodes")))
      yield DownstreamNode(k.split('.').last, v.split("\"").mkString(""))
  }
}

//todo replace with the actual config, scrubbed of passwords
case class ShrineConfig(isHub: Boolean,
                        hub: Hub,
                        pmEndpoint: Endpoint,
                        ontEndpoint: Endpoint,
                        hiveCredentials: HiveCredentials,
                        adapter: Adapter,
                        queryEntryPoint: QEP,
                        networkStatusQuery: String,
                        configMap: Map[String, String]
                       ) extends DefaultJsonSupport

object ShrineConfig extends DefaultJsonSupport {
  def apply(config: ParsedConfig): ShrineConfig = {
    val hub = Hub(config)
    val isHub = config.isHub
    val pmEndpoint = Endpoint("pm", config)
    val ontEndpoint = Endpoint("ont", config)
    val hiveCredentials = HiveCredentials(config)
    val adapter = Adapter(config)
    val queryEntryPoint = QEP(config)
    val networkStatusQuery = config.configMap("shrine.networkStatusQuery")

    ShrineConfig(isHub, hub, pmEndpoint, ontEndpoint, hiveCredentials, adapter, queryEntryPoint, networkStatusQuery, config.configMap)
  }
}

case class Endpoint(acceptAllCerts: Boolean, url: String, timeoutSeconds: Int)

object Endpoint {
  def apply(endpointType: String, parsedConfig: ParsedConfig): Endpoint = {
    val prefix = "shrine." + endpointType.toLowerCase + "Endpoint."

    val acceptAllCerts = parsedConfig.configMap.getOrElse(prefix + "acceptAllCerts", "") == "true"
    val url = parsedConfig.configMap.getOrElse(prefix + "url", "")
    val timeoutSeconds = parsedConfig.configMap.getOrElse(prefix + "timeout.seconds", "0").toInt
    Endpoint(acceptAllCerts, url, timeoutSeconds)
  }
}

case class HiveCredentials(domain: String,
                           username: String,
                           password: String,
                           crcProjectId: String,
                           ontProjectId: String)

object HiveCredentials {
  def apply(parsedConfig: ParsedConfig): HiveCredentials = {
    val key = "shrine.hiveCredentials."
    val domain = parsedConfig.configMap.getOrElse(key + "domain", "")
    val username = parsedConfig.configMap.getOrElse(key + "username", "")
    val password = "REDACTED"
    val crcProjectId = parsedConfig.configMap.getOrElse(key + "crcProjectId", "")
    val ontProjectId = parsedConfig.configMap.getOrElse(key + "ontProjectId", "")
    HiveCredentials(domain, username, password, crcProjectId, ontProjectId)
  }
}

// -- hub only -- //
//todo delete when the Dashboard front end can use the status service's hub method
case class Hub(shouldQuerySelf: Boolean,
               create: Boolean,
               downstreamNodes: Iterable[DownstreamNode])

object Hub {
  def apply(parsedConfig: ParsedConfig): Hub = {
    val shouldQuerySelf = parsedConfig.shouldQuerySelf
    val create = parsedConfig.isHub
    val downstreamNodes = DownstreamNode.create(parsedConfig.configMap)
    Hub(shouldQuerySelf, create, downstreamNodes)
  }
}

// -- adapter info -- //
case class Adapter(crcEndpointUrl: String, setSizeObfuscation: Boolean, adapterLockoutAttemptsThreshold: Int,
                   adapterMappingsFilename: String)

object Adapter {
  def apply(parsedConfig: ParsedConfig): Adapter = {
    val key = "shrine.adapter."
    val crcEndpointUrl = parsedConfig.configMap.getOrElse(key + "crcEndpoint.url", "")
    val setSizeObfuscation = parsedConfig.configMap.getOrElse(key + "setSizeObfuscation", "").toLowerCase == "true"
    val adapterLockoutAttemptsThreshold = parsedConfig.configMap.getOrElse(key + "adapterLockoutAttemptsThreshold", "0").toInt
    val adapterMappingsFileName = parsedConfig.configMap.getOrElse(key + "adapterMappingsFileName", "")

    Adapter(crcEndpointUrl, setSizeObfuscation, adapterLockoutAttemptsThreshold, adapterMappingsFileName)
  }
}


case class Steward(qepUserName: String, stewardBaseUrl: String)

object Steward {
  def apply(parsedConfig: ParsedConfig): Steward = {
    val key = "shrine.queryEntryPoint.shrineSteward."
    val qepUserName = parsedConfig.configMap.getOrElse(key + "qepUserName", "")
    val stewardBaseUrl = parsedConfig.configMap.getOrElse(key + "stewardBaseUrl", "")
    Steward(qepUserName, stewardBaseUrl)
  }
}


// -- if needed -- //
case class TimeoutInfo(timeUnit: String, description: String)


case class DatabaseInfo(createTablesOnStart: Boolean, dataSourceFrom: String,
                        jndiDataSourceName: String, slickProfileClassName: String)

case class Audit(database: DatabaseInfo, collectQepAudit: Boolean)

object Audit {
  def apply(parsedConfig: ParsedConfig): Audit = {
    val key = "shrine.queryEntryPoint.audit."
    val createTablesOnStart = parsedConfig.configMap.getOrElse(key + "database.createTablesOnStart", "") == "true"
    val dataSourceFrom = parsedConfig.configMap.getOrElse(key + "database.dataSourceFrom", "")
    val jndiDataSourceName = parsedConfig.configMap.getOrElse(key + "database.jndiDataSourceName", "")
    val slickProfileClassName = parsedConfig.configMap.getOrElse(key + "database.slickProfileClassName", "")
    val collectQepAudit = parsedConfig.configMap.getOrElse(key + "collectQepAudit", "") == "true"
    val database = DatabaseInfo(createTablesOnStart, dataSourceFrom, jndiDataSourceName, slickProfileClassName)
    Audit(database, collectQepAudit)
  }
}

case class QEP(
                maxQueryWaitTimeMinutes: Int,
                create: Boolean,
                attachSigningCert: Boolean,
                authorizationType: String,
                includeAggregateResults: Boolean,
                authenticationType: String,
                audit: Audit,
                shrineSteward: Steward,
                broadcasterServiceEndpointUrl: Option[String]
              )

object QEP {
  val key = "shrine.queryEntryPoint."

  def apply(parsedConfig: ParsedConfig): QEP = QEP(
    maxQueryWaitTimeMinutes = parsedConfig.configMap.getOrElse(key + "maxQueryWaitTime.minutes", "0").toInt,
    create = parsedConfig.configMap.getOrElse(key + "create", "") == "true",
    attachSigningCert = parsedConfig.configMap.getOrElse(key + "attachSigningCert", "") == "true",
    authorizationType = parsedConfig.configMap.getOrElse(key + "authorizationType", ""),
    includeAggregateResults = parsedConfig.configMap.getOrElse(key + "includeAggregateResults", "") == "true",
    authenticationType = parsedConfig.configMap.getOrElse(key + "authenticationType", ""),
    audit = Audit(parsedConfig),
    shrineSteward = Steward(parsedConfig),
    broadcasterServiceEndpointUrl = parsedConfig.configMap.get(key + "broadcasterServiceEndpoint.url")
  )
}

//adapted from https://gist.github.com/joseraya/176821d856b43b1cfe19
object gruntWatchCorsSupport extends Directive0 with RouteConcatenation {

  import spray.http.AllOrigins
  import spray.http.HttpHeaders.{`Access-Control-Allow-Headers`, `Access-Control-Allow-Methods`, `Access-Control-Allow-Origin`, `Access-Control-Max-Age`}
  import spray.http.HttpMethods.{GET, OPTIONS, POST}
  import spray.routing.directives.MethodDirectives.options
  import spray.routing.directives.RespondWithDirectives.respondWithHeaders
  import spray.routing.directives.RouteDirectives.complete

  private val allowOriginHeader = `Access-Control-Allow-Origin`(AllOrigins)
  private val optionsCorsHeaders = List(
    `Access-Control-Allow-Headers`("Origin, X-Requested-With, Content-Type, Accept, Accept-Encoding, Accept-Language, Host, Referer, User-Agent, Authorization"),
    `Access-Control-Max-Age`(1728000)) //20 days

  val gruntWatch: Boolean = ConfigSource.config.getBoolean("shrine.dashboard.gruntWatch")

  override def happly(f: (HNil) => Route): Route = {
    if (gruntWatch) {
      options {
        respondWithHeaders(`Access-Control-Allow-Methods`(OPTIONS, GET, POST) :: allowOriginHeader :: optionsCorsHeaders) {
          complete(StatusCodes.OK)
        }
      } ~ f(HNil)
    }
    else f(HNil)
  }
}