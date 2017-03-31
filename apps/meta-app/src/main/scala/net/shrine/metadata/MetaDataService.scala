package net.shrine.metadata

import net.shrine.log.Loggable
import spray.routing.{HttpService, _}

/**
  * An outer API to mix in sub services
  */
trait MetaDataService extends HttpService
  with StaticDataService
  with Loggable {
  val shrineInfo =
    """
      |The SHRINE Metadata service.
      |
      |This web API gives you access to sub-services within this shrine node.
      |You can access these services by calling shrine-medadata/[service name].
      |You can learn more about each service by calling shrine-metadata/[service name]
      |for top-level information about each.
    """.stripMargin

  lazy val route: Route = pingRoute ~ staticDataRoute

  lazy val pingRoute: Route = get {
    pathPrefix("ping") { complete("pong")}
  }
}
