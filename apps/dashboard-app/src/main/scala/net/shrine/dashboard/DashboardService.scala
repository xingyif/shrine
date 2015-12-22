package net.shrine.dashboard

import akka.actor.{ActorSystem, Actor}
import akka.event.Logging
import net.shrine.authentication.UserAuthenticator

import net.shrine.authorization.steward.OutboundUser
import net.shrine.dashboard.jwtauth.ShrineJwtAuthenticator
import net.shrine.i2b2.protocol.pm.User
import net.shrine.dashboard.httpclient.HttpClientDirectives.forwardUnmatchedPath
import net.shrine.log.Loggable
import shapeless.HNil

import spray.http.{HttpResponse, HttpRequest, StatusCodes}
import spray.httpx.Json4sSupport
import spray.routing.directives.LogEntry
import spray.routing.{AuthenticationFailedRejection, Rejected, RouteConcatenation, Directive0, Route, HttpService}

import org.json4s.{DefaultFormats, Formats}

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
    redirectToIndex ~ staticResources ~ makeTrouble ~ about ~ authenticatedInBrowser ~ authenticateRemoteDashboard
  }

  // logs just the request method, uri and response at info level
  def logEntryForRequestResponse(req: HttpRequest): Any => Option[LogEntry] = {
    case res: HttpResponse => {
      Some(LogEntry(s"\n  Request: $req \n  Response: $res", Logging.InfoLevel))
    }
    case _ => None // other kind of responses
  }

  // logs just the request method, uri and response status at info level
  def logEntryForRequest(req: HttpRequest): Any => Option[LogEntry] = {
    case res: HttpResponse => {
      Some(LogEntry(s"\n  Request: $req \n  Response status: ${res.status}", Logging.InfoLevel))
    }
    case _ => None // other kind of responses
  }

  //pathPrefixTest shields the QEP code from the redirect.
  def authenticatedInBrowser: Route = pathPrefixTest("user"|"admin") {
    logRequestResponse(logEntryForRequestResponse _) { //logging is controlled by Akka's config, slf4j, and log4j config
      reportIfFailedToAuthenticate {
        authenticate(userAuthenticator.basicUserAuthenticator) { user =>
          pathPrefix("user") {
            userRoute(user)
          } ~
            pathPrefix("admin") {
              adminRoute(user)
            }
        }
      }
    }
  }

  val reportIfFailedToAuthenticate = routeRouteResponse {
    case Rejected(List(AuthenticationFailedRejection(_,_))) =>
      complete("AuthenticationFailed")
  }

  def authenticateRemoteDashboard:Route = pathPrefix("remoteDashboard") {
    logRequestResponse(logEntryForRequestResponse _) { //logging is controlled by Akka's config, slf4j, and log4j config
      authenticate(ShrineJwtAuthenticator.theAuthenticator) { user =>
        adminRoute(user)
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

      implicit val system = ActorSystem("sprayServer")
      forwardUnmatchedPath(happyBaseUrl)
    } ~
    pathPrefix("ping") { complete("pong")}
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
