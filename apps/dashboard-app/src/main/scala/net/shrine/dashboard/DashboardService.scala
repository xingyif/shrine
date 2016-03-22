package net.shrine.dashboard

import akka.actor.Actor
import akka.event.Logging
import net.shrine.authentication.UserAuthenticator

import net.shrine.authorization.steward.OutboundUser
import net.shrine.dashboard.jwtauth.ShrineJwtAuthenticator
import net.shrine.i2b2.protocol.pm.User
import net.shrine.status.protocol.{Config => StatusProtocolConfig}
import net.shrine.dashboard.httpclient.HttpClientDirectives.{forwardUnmatchedPath,requestUriThenRoute}
import net.shrine.log.Loggable
import shapeless.HNil

import spray.http.{Uri, HttpResponse, HttpRequest, StatusCodes}
import spray.httpx.Json4sSupport
import spray.routing.directives.LogEntry
import spray.routing.{AuthenticationFailedRejection, Rejected, RouteConcatenation, Directive0, Route, HttpService}

import org.json4s.{DefaultFormats, Formats}
import org.json4s.native.JsonMethods.{parse => json4sParse}

import scala.concurrent.ExecutionContext.Implicits.global

// we don't implement our route structure directly in the service actor because
// we want to be able to test it independently, without having to spin up an actor

class DashboardServiceActor extends Actor with DashboardService {

  // the HttpService trait defines only one abstract member, which
  // connects the services environment to the enclosing actor or test
  def actorRefFactory = context

  // this actor only runs our route, but you could add
  // other things here, like request stream processing
  // or timeout handling
  def receive = runRoute(route)
}

// this trait defines our service behavior independently from the service actor
trait DashboardService extends HttpService with Json4sSupport with Loggable {
  implicit def json4sFormats: Formats = DefaultFormats

  val userAuthenticator = UserAuthenticator(DashboardConfigSource.config)

  //don't need to do anything special for unauthorized users, but they do need access to a static form.
  lazy val route:Route = gruntWatchCorsSupport{
    redirectToIndex ~ staticResources ~ makeTrouble ~ about ~ authenticatedInBrowser ~ authenticatedDashboard
  }

  // logs just the request method, uri and response at info level
  def logEntryForRequestResponse(req: HttpRequest): Any => Option[LogEntry] = {
    case res: HttpResponse => Some(LogEntry(s"\n  Request: $req\n  Response: $res", Logging.InfoLevel))
    case _ => None // other kind of responses
  }

  // logs just the request method, uri and response status at info level
  def logEntryForRequest(req: HttpRequest): Any => Option[LogEntry] = {
    case res: HttpResponse => Some(LogEntry(s"\n  Request: $req\n  Response status: ${res.status}", Logging.InfoLevel))
    case _ => None // other kind of responses
  }

  //pathPrefixTest shields the QEP code from the redirect.
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

  //todo is this an admin? Does it matter?
  def adminRoute(user:User):Route = get {

    pathPrefix("happy") {
      val happyBaseUrl: String = DashboardConfigSource.config.getString("shrine.dashboard.happyBaseUrl")

      forwardUnmatchedPath(happyBaseUrl)
    } ~
    pathPrefix("messWithHappyVersion") {
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
    pathPrefix("options"){getOptions}~
    pathPrefix("summary"){getSummary}
  }

  lazy val getConfig:Route = {

    // -- vars -- //
    val urlKey                = "shrine.dashboard.statusBaseUrl"
    val statusBaseUrl: String = DashboardConfigSource.config.getString(urlKey)

    def completeSummaryRoute(httpResponse:HttpResponse,uri:Uri):Route = {
      ctx => {
        val config =  ShrineParser.parseShrineFromConfig(httpResponse.entity.asString)


        ctx.complete(
          ShrineConfig(config)
        )
      }
    }

    requestUriThenRoute(statusBaseUrl + "/config", completeSummaryRoute)
  }

  lazy val getClasspath:Route = {
    val statusBaseUrl: String = DashboardConfigSource.config.getString("shrine" +
      ".dashboard" +
      ".statusBaseUrl")

    def pullClasspathFromConfig(httpResponse:HttpResponse,uri:Uri):Route = {
      ctx => {
        // -- import parser -- //
        import org.json4s.native.JsonMethods.parse

        // -- vars -- //
        val result        = httpResponse.entity.asString
        val config        = parse(result)
          .extract[net.shrine.status.protocol.Config]
          .keyValues
          .filterKeys(_.toLowerCase.startsWith("shrine"))
        val shrineConfig  = ShrineConfig(config)

        // -- complete route -- //
        ctx.complete(shrineConfig)
      }
    }

    // -- init request -- //
    requestUriThenRoute(statusBaseUrl + "/config",pullClasspathFromConfig)
  }




  lazy val getOptions:Route = {

    // -- vars -- //
    val urlKey                = "shrine.dashboard.statusBaseUrl"
    val statusBaseUrl: String = DashboardConfigSource.config.getString(urlKey)

    def completeSummaryRoute(httpResponse:HttpResponse,uri:Uri):Route = {
      ctx => {
        val config =  ShrineParser.parseShrineFromConfig(httpResponse.entity.asString)

        ctx.complete(
          Options(config)
        )
      }
    }

    requestUriThenRoute(statusBaseUrl + "/config", completeSummaryRoute)
  }


  lazy val getSummary:Route = {

    val happyBaseUrl: String = DashboardConfigSource.config.getString("shrine.dashboard.happyBaseUrl")

    def pullSummaryFromHappy(httpResponse:HttpResponse,uri:Uri):Route = {
      ctx => {
        val result = scala.xml.XML.loadString(httpResponse.entity.asString)
        val isHub = (result \\ "notAHub").text.length == 0
        val shrineVersion =   (result \\ "versionInfo" \ "shrineVersion").text
        val shrineBuildDate = (result \\ "versionInfo" \ "buildDate").text
        val ontologyVersion = (result \\ "versionInfo" \ "ontologyVersion").text
        val ontologyTerm = (result \\ "adapter" \\ "queryDefinition" \\ "term").text



        def isHubOk:Boolean = {
          if(!isHub) true
          else {
            val hasFailures = (result \\ "net" \ "failureCount").text.toInt > 0
            val hasInvalidResults = (result \\ "net" \ "validResultCount").text.toInt != (result \\ "net" \ "expectedResultCount").text.toInt
            val hasTimeouts = (result \\ "net" \ "timeoutCount").text.toInt > 0
            !hasFailures && !hasInvalidResults && !hasTimeouts
          }
        }

        val hubOk = isHubOk
        val adapterOk = (result \\ "adapter" \\ "errorResponse").length == 0
        val keystoreOk = true
        val qepOk = true

        val summary = Summary(isHub, shrineVersion, shrineBuildDate, ontologyVersion, ontologyTerm, adapterOk, keystoreOk, hubOk, qepOk)

        ctx.complete(summary)
      }
    }

    requestUriThenRoute(happyBaseUrl+"/all", pullSummaryFromHappy)
  }
}


case class Summary(isHub:Boolean, shrineVersion:String, shrineBuildDate:String, ontologyVersion:String,
                   ontologyTerm:String, adapterOk:Boolean, keystoreOk:Boolean, hubOk:Boolean, qepOk:Boolean) {

}




/**
 * Centralized parsing logic for map of shrine.conf
 * the class literal `T.class` in Java.
 */
object ShrineParser{

  // -- @todo: need to make sure this is initialized. -- //
  private var shrineMap:Map[String, String]
  = Map(""->"")
  private val trueVal = "true"
  private val rootKey = "shrine"

  private def isInit =
    this.shrineMap.contains(rootKey)


  // -- @todo: where should this live ? -- //
  def parseShrineFromConfig(resultStr:String) = {

    // -- needed to use json4s parse -- //
    implicit def json4sFormats: Formats = DefaultFormats

    // -- extract map of shrine subset of config file -- //
    this.shrineMap = json4sParse(resultStr).extract[StatusProtocolConfig].keyValues
      .filterKeys(_.toLowerCase.startsWith("shrine"))

    this.shrineMap
  }

  // --  @todo throw exception in else? -- //
  private def Parser =
    this.shrineMap

  // -- -- //
  def IsHub =
    getOrElse(rootKey + ".hub.create", "")
      .toLowerCase == trueVal

  // -- -- //
  def StewardEnabled =
    Parser.keySet
      .contains(rootKey + ".queryEntryPoint.shrineSteward")

  // -- -- //
  def ShouldQuerySelf =
    getOrElse(rootKey + ".hub.shouldQuerySelf", "")
      .toLowerCase == trueVal

  // -- -- //
  def DownstreamNodes =
    for((k,v) <- Parser.filterKeys(_.toLowerCase.startsWith
      ("shrine.hub.downstreamnodes"))) yield DownstreamNode(k.split('.').last,
      v.split("\"").mkString(""))

  // -- -- //
  def getOrElse(key:String, elseVal:String = "") = this.shrineMap.getOrElse(key, elseVal)
    .split("\"").mkString("")
}

case class DownstreamNode(name:String, url:String){
}


case class Options(isHub:Boolean, stewardEnabled:Boolean, shouldQuerySelf:Boolean,
                   downstreamNodes:Iterable[DownstreamNode])
object Options{
  def apply(configMap:Map[String, String]):Options ={
    val isHub           = ShrineParser.IsHub
    val stewardEnabled  = ShrineParser.StewardEnabled
    val shouldQuerySelf = ShrineParser.ShouldQuerySelf
    val downstreamNodes = ShrineParser.DownstreamNodes

    Options(isHub, stewardEnabled, shouldQuerySelf, downstreamNodes)
  }
}

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
  def apply(configMap:Map[String, String]):ShrineConfig = {
    val hub               = Hub(configMap)
    val isHub             = ShrineParser.IsHub
    val pmEndpoint        = Endpoint("pm")
    val ontEndpoint       = Endpoint("ont")
    val hiveCredentials   = HiveCredentials()
    val adapter           = Adapter()
    val queryEntryPoint   = QEP()
    val networkStatusQuery = configMap("shrine.networkStatusQuery")

    ShrineConfig(isHub, hub, pmEndpoint, ontEndpoint, hiveCredentials, adapter, queryEntryPoint, networkStatusQuery)
  }
}

case class Endpoint(acceptAllCerts:Boolean, url:String, timeoutSeconds:Int)
object Endpoint{
  def apply(endpointType:String):Endpoint = {
    val prefix = "shrine." + endpointType.toLowerCase + "Endpoint."

    val acceptAllCerts  = ShrineParser.getOrElse(prefix + "acceptAllCerts", "") == "true"
    val url             = ShrineParser.getOrElse(prefix + "url")
    val timeoutSeconds  = ShrineParser.getOrElse(prefix + "timeout.seconds", "0").toInt
    Endpoint(acceptAllCerts, url, timeoutSeconds)
  }
}

case class HiveCredentials(domain:String, username:String, password:String,
                           crcProjectId:String, ontProjectId:String)
object HiveCredentials{
  def apply():HiveCredentials = {
    val key           = "shrine.hiveCredentials."
    val domain        = ShrineParser.getOrElse(key + "domain")
    val username      = ShrineParser.getOrElse(key + "username")
    val password      = "REDACTED"
    val crcProjectId  = ShrineParser.getOrElse(key + "crcProjectId")
    val ontProjectId  = ShrineParser.getOrElse(key + "ontProjectId")
    HiveCredentials(domain, username, password, crcProjectId, ontProjectId)
  }
}

// -- hub only -- //
case class Hub(shouldQuerySelf:Boolean, create:Boolean,
               downstreamNodes:Iterable[DownstreamNode])
object Hub{
  def apply(configMap:Map[String,String]):Hub = {
    val shouldQuerySelf = ShrineParser.ShouldQuerySelf
    val create          = ShrineParser.IsHub
    val downstreamNodes = ShrineParser.DownstreamNodes
    Hub(shouldQuerySelf, create, downstreamNodes)
  }
}

// -- adapter info -- //
case class Adapter(crcEndpointUrl:String, setSizeObfuscation:Boolean, adapterLockoutAttemptsThreshold:Int,
                   adapterMappingsFilename:String)
object Adapter{
  def apply():Adapter = {
    val key                             = "shrine.adapter."
    val crcEndpointUrl                  = ShrineParser.getOrElse(key + "crcEndpoint.url")
    val setSizeObfuscation              = ShrineParser.getOrElse(key + "setSizeObfuscation").toLowerCase == "true"
    val adapterLockoutAttemptsThreshold = ShrineParser.getOrElse(key + "adapterLockoutAttemptsThreshold", "0").toInt
    val adapterMappingsFileName         = ShrineParser.getOrElse(key + "adapterMappingsFileName")

    Adapter(crcEndpointUrl, setSizeObfuscation, adapterLockoutAttemptsThreshold, adapterMappingsFileName)
  }
}


case class Steward(qepUserName:String, stewardBaseUrl:String)
object Steward {
  def apply ():Steward = {
    val key = "shrine.queryEntryPoint.shrineSteward."
    val qepUserName     = ShrineParser.getOrElse(key + "qepUserName")
    val stewardBaseUrl  = ShrineParser.getOrElse(key + "stewardBaseUrl")
    Steward(qepUserName, stewardBaseUrl)
  }
}


// -- if needed -- //
case class TimeoutInfo (timeUnit:String, description:String)


case class DatabaseInfo(createTablesOnStart:Boolean, dataSourceFrom:String,
                        jndiDataSourceName:String, slickProfileClassName:String)
case class Audit(database:DatabaseInfo, collectQepAudit:Boolean)
object Audit{
  def apply():Audit = {
    val key = "shrine.queryEntryPoint.audit."
    val createTablesOnStart     = ShrineParser.getOrElse(key + "database.createTablesOnStart") == "true"
    val dataSourceFrom          = ShrineParser.getOrElse(key + "database.dataSourceFrom")
    val jndiDataSourceName      = ShrineParser.getOrElse(key + "database.jndiDataSourceName")
    val slickProfileClassName   = ShrineParser.getOrElse(key + "database.slickProfileClassName")
    val collectQepAudit         = ShrineParser.getOrElse(key + "collectQepAudit") == "true"
    val database = DatabaseInfo(createTablesOnStart, dataSourceFrom, jndiDataSourceName, slickProfileClassName)
    Audit(database, collectQepAudit)
  }
}
case class QEP(maxQueryWaitTimeMinutes:Int, create:Boolean,attachSigningCert:Boolean,
               authorizationType:String, includeAggregateResults:Boolean, authenticationType:String, audit:Audit, shrineSteward:Steward)
object QEP{
  def apply():QEP = {
    val key = "shrine.queryEntryPoint."
    val maxQueryWaitTimeMinutes = ShrineParser.getOrElse(key + "maxQueryWaitTime.minutes", "0").toInt
    val create                  = ShrineParser.getOrElse(key + "create") == "true"
    val attachSigningCert       = ShrineParser.getOrElse(key + "attachSigningCert") == "true"
    val authorizationType       = ShrineParser.getOrElse(key + "authorizationType")
    val includeAggregateResults = ShrineParser.getOrElse(key + "includeAggregateResults") == "true"
    val authenticationType      = ShrineParser.getOrElse(key + "authenticationType", "")
    val audit                   = Audit()
    val shrineSteward           = Steward()
    QEP(maxQueryWaitTimeMinutes, create, attachSigningCert, authorizationType, includeAggregateResults, authenticationType, audit, shrineSteward)
  }
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
