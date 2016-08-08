package net.shrine.dashboard

import akka.actor.Actor
import akka.event.Logging
import net.shrine.authentication.UserAuthenticator
import net.shrine.authorization.steward.OutboundUser
import net.shrine.dashboard.jwtauth.ShrineJwtAuthenticator
import net.shrine.i2b2.protocol.pm.User
import net.shrine.status.protocol.{Config => StatusProtocolConfig}
import net.shrine.dashboard.httpclient.HttpClientDirectives.{forwardUnmatchedPath, requestUriThenRoute}
import net.shrine.log.Loggable
import net.shrine.problem.{ProblemDigest, Problems}
import net.shrine.serialization.NodeSeqSerializer
import shapeless.HNil
import spray.http.{HttpRequest, HttpResponse, StatusCodes, Uri}
import spray.httpx.Json4sSupport
import spray.routing.directives.LogEntry
import spray.routing._
import org.json4s.{DefaultFormats, Formats}
import org.json4s.native.JsonMethods.{parse => json4sParse}
import org.json4s.native.Serialization._

import scala.collection.immutable.Iterable
import scala.concurrent.duration.{Duration, FiniteDuration, SECONDS}
import scala.concurrent.ExecutionContext.Implicits.global

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

trait DashboardService extends HttpService with Json4sSupport with Loggable {
  implicit def json4sFormats: Formats = DefaultFormats

  val userAuthenticator = UserAuthenticator(DashboardConfigSource.config)

  //don't need to do anything special for unauthorized users, but they do need access to a static form.
  lazy val route:Route = gruntWatchCorsSupport{
    redirectToIndex ~ staticResources ~ makeTrouble ~ about ~ authenticatedInBrowser ~ authenticatedDashboard
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

  def authenticatedInBrowser: Route = pathPrefixTest("user"|"admin"|"toDashboard") {
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
    case Rejected(List(AuthenticationFailedRejection(_,_))) =>
      complete("AuthenticationFailed")
  }

  def authenticatedDashboard:Route = pathPrefix("fromDashboard") {
    logRequestResponse(logEntryForRequestResponse _) { //logging is controlled by Akka's config, slf4j, and log4j config
      get { //all remote dashboard calls are gets.
        authenticate(ShrineJwtAuthenticator.authenticate) { user =>
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
    ( path("index.html") | pathSingleSlash) {
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
    complete("Nothing here yet") //todo
  }

  def userRoute(user:User):Route = get {
    pathPrefix("whoami") {
      complete(OutboundUser.createFromUser(user))
    }
  }

  //todo check that this an admin.
  def adminRoute(user:User):Route = get {

    pathPrefix("happy") {
      val happyBaseUrl: String = DashboardConfigSource.config.getString("shrine.dashboard.happyBaseUrl")

      forwardUnmatchedPath(happyBaseUrl)
    } ~
    pathPrefix("messWithHappyVersion") { //todo is this used?
      val happyBaseUrl: String = DashboardConfigSource.config.getString("shrine.dashboard.happyBaseUrl")

      def pullClasspathFromConfig(httpResponse:HttpResponse,uri:Uri):Route = {
        ctx => {
          val result = httpResponse.entity.asString
          ctx.complete(s"Got '$result' from $uri")
        }
      }

      requestUriThenRoute(happyBaseUrl+"/version",pullClasspathFromConfig)
    } ~
    pathPrefix("ping") {complete("pong")}~
    pathPrefix("status"){statusRoute(user)}
  }

  //Manually test this by running a curl command
  //curl -k -w "\n%{response_code}\n" -u dave:kablam "https://shrine-dev1.catalyst:6443/shrine-dashboard/toDashboard/shrine-dev2.catalyst/shrine-dashboard/fromDashboard/ping"
  /**
    * Forward a request from this dashboard to a remote dashboard
    */
  def toDashboardRoute(user:User):Route = get {

    pathPrefix(Segment) { dnsName =>
      val remoteDashboardProtocol = DashboardConfigSource.config.getString("shrine.dashboard.remoteDashboard.protocol")
      val remoteDashboardPort = DashboardConfigSource.config.getString("shrine.dashboard.remoteDashboard.port")
      val remoteDashboardPathPrefix = DashboardConfigSource.config.getString("shrine.dashboard.remoteDashboard.pathPrefix")

      val baseUrl = s"$remoteDashboardProtocol$dnsName$remoteDashboardPort/$remoteDashboardPathPrefix"

      forwardUnmatchedPath(baseUrl,Some(ShrineJwtAuthenticator.createOAuthCredentials(user)))
    }
  }

  def statusRoute(user:User):Route = get {
    pathPrefix("config"){getConfig}~
    pathPrefix("classpath"){getClasspath}~
    pathPrefix("options"){getOptionalParts}~  //todo rename path to optionalParts
    pathPrefix("summary"){getSummary}~
    pathPrefix("problems"){getProblems}
  }

  val statusBaseUrl = DashboardConfigSource.config.getString("shrine.dashboard.statusBaseUrl")

  lazy val getConfig:Route = {

    def completeConfigRoute(httpResponse:HttpResponse,uri:Uri):Route = {
      ctx => {
        val config = ParsedConfig(httpResponse.entity.asString)

        ctx.complete(
          ShrineConfig(config)
        )
      }
    }

    requestUriThenRoute(statusBaseUrl + "/config", completeConfigRoute)
  }

  lazy val getClasspath:Route = {

    def pullClasspathFromConfig(httpResponse:HttpResponse,uri:Uri):Route = {
      ctx => {
        val result        = httpResponse.entity.asString
        val shrineConfig  = ShrineConfig(ParsedConfig(result))

        ctx.complete(shrineConfig)
      }
    }

    requestUriThenRoute(statusBaseUrl + "/config",pullClasspathFromConfig)
  }

  lazy val getOptionalParts:Route = {
    requestUriThenRoute(statusBaseUrl + "/optionalParts")
  }

  lazy val getSummary:Route = {
    requestUriThenRoute(statusBaseUrl + "/summary")
  }

  // table based view, can see N problems at a time. Front end sends how many problems that they want
  // to skip, and it will take N the 'nearest N' ie with n = 20, 0-19 -> 20, 20-39 -> 20-40
  lazy val getProblems:Route = {

    def floorMod(x: Int, y: Int) = {
      x - (x % y)
    }

    val formats = DefaultFormats + new NodeSeqSerializer

    parameter("offset" ? "0") { offsetString: String =>
      val n = 20
      // TODO: Once Bamboo/Deploy is running Java 8, switch to using Math.floorMod


      // Try and grab the offset. If a number wasn't passed in, just default to 0
      val offset = try { floorMod(Math.max(0, offsetString.toInt), n) } catch { case a:java.lang.NumberFormatException =>
        println(s"Could not parse problems GET request parameter, received $offsetString, threw $a")
        0
      }
      val p = Problems
      val db = p.DatabaseConnector
      val timeout: Duration = new FiniteDuration(15, SECONDS)
      complete(Problems.slickProfile.getClass)
//      val problemsAndSize: (Seq[ProblemDigest], Int) = db.runBlocking(db.IO.sizeAndProblemDigest(n, offset))(timeout)
//      val response = ProblemResponse(problemsAndSize._2, offset, n, problemsAndSize._1)
      //todo: Find a better way to do this besides writing and parsing the json response
      //complete(json4sParse(write(response)(formats)))
    }
  }

}

/**
 * Centralized parsing logic for map of shrine.conf
 * the class literal `T.class` in Java.
 */
//todo most of this info should come directly from the status service in Shrine, not from reading the config
case class ParsedConfig(configMap:Map[String, String]){

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

  def fromJsonString(jsonString:String): String = jsonString.split("\"").mkString("")

  def get(key:String): Option[String] = configMap.get(key).map(fromJsonString)

  def getOrElse(key:String, elseVal:String = ""): String = get(key).getOrElse(elseVal)
}

object ParsedConfig {
  def apply(jsonString:String):ParsedConfig = {

    implicit def json4sFormats: Formats = DefaultFormats

    ParsedConfig(json4sParse(jsonString).extract[StatusProtocolConfig].keyValues.filterKeys(_.toLowerCase.startsWith("shrine")))
  }

}

case class DownstreamNode(name:String, url:String)

object DownstreamNode {
  def create(configMap:Map[String,String]):Iterable[DownstreamNode] = {
    for ((k, v) <- configMap.filterKeys(_.toLowerCase.startsWith
    ("shrine.hub.downstreamnodes")))
      yield DownstreamNode(k.split('.').last,v.split("\"").mkString(""))
  }
}

case class ProblemResponse(size: Int, offset: Int, n: Int, problems: Seq[ProblemDigest])

//todo replace with the actual config, scrubbed of passwords
case class ShrineConfig(isHub:Boolean,
                        hub:Hub,
                        pmEndpoint:Endpoint,
                        ontEndpoint:Endpoint,
                        hiveCredentials: HiveCredentials,
                        adapter: Adapter,
                        queryEntryPoint:QEP,
                        networkStatusQuery:String
                       )

object ShrineConfig{
  def apply(config:ParsedConfig):ShrineConfig = {
    val hub               = Hub(config)
    val isHub             = config.isHub
    val pmEndpoint        = Endpoint("pm",config)
    val ontEndpoint       = Endpoint("ont",config)
    val hiveCredentials   = HiveCredentials(config)
    val adapter           = Adapter(config)
    val queryEntryPoint   = QEP(config)
    val networkStatusQuery = config.configMap("shrine.networkStatusQuery")

    ShrineConfig(isHub, hub, pmEndpoint, ontEndpoint, hiveCredentials, adapter, queryEntryPoint, networkStatusQuery)
  }
}

case class Endpoint(acceptAllCerts:Boolean, url:String, timeoutSeconds:Int)
object Endpoint{
  def apply(endpointType:String,parsedConfig:ParsedConfig):Endpoint = {
    val prefix = "shrine." + endpointType.toLowerCase + "Endpoint."

    val acceptAllCerts  = parsedConfig.configMap.getOrElse(prefix + "acceptAllCerts", "") == "true"
    val url             = parsedConfig.configMap.getOrElse(prefix + "url","")
    val timeoutSeconds  = parsedConfig.configMap.getOrElse(prefix + "timeout.seconds", "0").toInt
    Endpoint(acceptAllCerts, url, timeoutSeconds)
  }
}

case class HiveCredentials(domain:String,
                           username:String,
                           password:String,
                           crcProjectId:String,
                           ontProjectId:String)
object HiveCredentials{
  def apply(parsedConfig:ParsedConfig):HiveCredentials = {
    val key           = "shrine.hiveCredentials."
    val domain        = parsedConfig.configMap.getOrElse(key + "domain","")
    val username      = parsedConfig.configMap.getOrElse(key + "username","")
    val password      = "REDACTED"
    val crcProjectId  = parsedConfig.configMap.getOrElse(key + "crcProjectId","")
    val ontProjectId  = parsedConfig.configMap.getOrElse(key + "ontProjectId","")
    HiveCredentials(domain, username, password, crcProjectId, ontProjectId)
  }
}

// -- hub only -- //
//todo delete when the Dashboard front end can use the status service's hub method
case class Hub(shouldQuerySelf:Boolean,
               create:Boolean,
               downstreamNodes:Iterable[DownstreamNode])
object Hub{
  def apply(parsedConfig:ParsedConfig):Hub = {
    val shouldQuerySelf = parsedConfig.shouldQuerySelf
    val create          = parsedConfig.isHub
    val downstreamNodes = DownstreamNode.create(parsedConfig.configMap)
    Hub(shouldQuerySelf, create, downstreamNodes)
  }
}

// -- adapter info -- //
case class Adapter(crcEndpointUrl:String, setSizeObfuscation:Boolean, adapterLockoutAttemptsThreshold:Int,
                   adapterMappingsFilename:String)
object Adapter{
  def apply(parsedConfig:ParsedConfig):Adapter = {
    val key                             = "shrine.adapter."
    val crcEndpointUrl                  = parsedConfig.configMap.getOrElse(key + "crcEndpoint.url","")
    val setSizeObfuscation              = parsedConfig.configMap.getOrElse(key + "setSizeObfuscation","").toLowerCase == "true"
    val adapterLockoutAttemptsThreshold = parsedConfig.configMap.getOrElse(key + "adapterLockoutAttemptsThreshold", "0").toInt
    val adapterMappingsFileName         = parsedConfig.configMap.getOrElse(key + "adapterMappingsFileName","")

    Adapter(crcEndpointUrl, setSizeObfuscation, adapterLockoutAttemptsThreshold, adapterMappingsFileName)
  }
}


case class Steward(qepUserName:String, stewardBaseUrl:String)
object Steward {
  def apply (parsedConfig:ParsedConfig):Steward = {
    val key = "shrine.queryEntryPoint.shrineSteward."
    val qepUserName     = parsedConfig.configMap.getOrElse(key + "qepUserName","")
    val stewardBaseUrl  = parsedConfig.configMap.getOrElse(key + "stewardBaseUrl","")
    Steward(qepUserName, stewardBaseUrl)
  }
}


// -- if needed -- //
case class TimeoutInfo (timeUnit:String, description:String)


case class DatabaseInfo(createTablesOnStart:Boolean, dataSourceFrom:String,
                        jndiDataSourceName:String, slickProfileClassName:String)
case class Audit(database:DatabaseInfo, collectQepAudit:Boolean)
object Audit{
  def apply(parsedConfig:ParsedConfig):Audit = {
    val key = "shrine.queryEntryPoint.audit."
    val createTablesOnStart     = parsedConfig.configMap.getOrElse(key + "database.createTablesOnStart","") == "true"
    val dataSourceFrom          = parsedConfig.configMap.getOrElse(key + "database.dataSourceFrom","")
    val jndiDataSourceName      = parsedConfig.configMap.getOrElse(key + "database.jndiDataSourceName","")
    val slickProfileClassName   = parsedConfig.configMap.getOrElse(key + "database.slickProfileClassName","")
    val collectQepAudit         = parsedConfig.configMap.getOrElse(key + "collectQepAudit","") == "true"
    val database = DatabaseInfo(createTablesOnStart, dataSourceFrom, jndiDataSourceName, slickProfileClassName)
    Audit(database, collectQepAudit)
  }
}
case class QEP(
            maxQueryWaitTimeMinutes:Int,
            create:Boolean,
            attachSigningCert:Boolean,
            authorizationType:String,
            includeAggregateResults:Boolean,
            authenticationType:String,
            audit:Audit,
            shrineSteward:Steward,
            broadcasterServiceEndpointUrl:Option[String]
)

object QEP{
  val key = "shrine.queryEntryPoint."
  def apply(parsedConfig:ParsedConfig):QEP = QEP(
    maxQueryWaitTimeMinutes = parsedConfig.configMap.getOrElse(key + "maxQueryWaitTime.minutes", "0").toInt,
    create                  = parsedConfig.configMap.getOrElse(key + "create","") == "true",
    attachSigningCert       = parsedConfig.configMap.getOrElse(key + "attachSigningCert","") == "true",
    authorizationType       = parsedConfig.configMap.getOrElse(key + "authorizationType",""),
    includeAggregateResults = parsedConfig.configMap.getOrElse(key + "includeAggregateResults","") == "true",
    authenticationType      = parsedConfig.configMap.getOrElse(key + "authenticationType", ""),
    audit                   = Audit(parsedConfig),
    shrineSteward           = Steward(parsedConfig),
    broadcasterServiceEndpointUrl = parsedConfig.configMap.get(key + "broadcasterServiceEndpoint.url")
  )
}

//adapted from https://gist.github.com/joseraya/176821d856b43b1cfe19
object gruntWatchCorsSupport extends Directive0 with RouteConcatenation {

  import spray.http.HttpHeaders.{`Access-Control-Allow-Methods`, `Access-Control-Max-Age`, `Access-Control-Allow-Headers`,`Access-Control-Allow-Origin`}
  import spray.routing.directives.RespondWithDirectives.respondWithHeaders
  import spray.routing.directives.MethodDirectives.options
  import spray.routing.directives.RouteDirectives.complete
  import spray.http.HttpMethods.{OPTIONS,GET,POST}
  import spray.http.AllOrigins

  private val allowOriginHeader = `Access-Control-Allow-Origin`(AllOrigins)
  private val optionsCorsHeaders = List(
    `Access-Control-Allow-Headers`("Origin, X-Requested-With, Content-Type, Accept, Accept-Encoding, Accept-Language, Host, Referer, User-Agent, Authorization"),
    `Access-Control-Max-Age`(1728000)) //20 days

  val gruntWatch:Boolean = DashboardConfigSource.config.getBoolean("shrine.dashboard.gruntWatch")

  override def happly(f: (HNil) => Route): Route = {
    if(gruntWatch) {
      options {
        respondWithHeaders(`Access-Control-Allow-Methods`(OPTIONS, GET, POST) ::  allowOriginHeader :: optionsCorsHeaders){
          complete(StatusCodes.OK)
        }
      } ~ f(HNil)
    }
    else f(HNil)
  }
}
