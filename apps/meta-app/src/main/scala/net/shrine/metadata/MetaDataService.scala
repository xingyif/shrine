package net.shrine.metadata

import akka.event.Logging
import net.shrine.authentication.UserAuthenticator
import net.shrine.i2b2.protocol.pm.User
import net.shrine.log.Loggable
import net.shrine.source.ConfigSource
import spray.http.{HttpRequest, HttpResponse}
import spray.routing.directives.LogEntry
import spray.routing.{HttpService, _}

import scala.concurrent.ExecutionContext

/**
  * An outer API to mix in sub services
  */
trait MetaDataService extends HttpService
  with StaticDataService
  with QepService
  with HornetQMomWebApi
  with Loggable {

  lazy val route: Route = logRequestResponse(logEntryForRequestResponse _) {
    //logging is controlled by Akka's config, slf4j, and log4j config
    metaDataRoute ~
      hornetQMomRoute ~
      staticDataRoute ~
      authenticatedRoute
  }

  //todo use this
  val shrineInfo =
    """
      |The SHRINE Metadata service.
      |
      |This web API gives you access to sub-services within this shrine node.
      |You can access these services by calling shrine-medadata/[service name].
      |You can learn more about each service by calling shrine-metadata/[service name]
      |for top-level information about each.
    """.stripMargin

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

  /****/
  lazy val metaDataRoute: Route = get {
    path("ping") { complete("pong")} ~
      pathEnd {complete(shrineInfo)}
  }

  lazy val authenticatedRoute: Route = authenticate(userAuthenticator.basicUserAuthenticator) { user:User =>
    qepRoute(user)
  }

  lazy val hornetQMomRoute: Route = momRoute

  lazy val userAuthenticator = UserAuthenticator(ConfigSource.config)

  implicit val ec: ExecutionContext

}