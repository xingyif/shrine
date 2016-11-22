package net.shrine.metadata

import com.typesafe.config.ConfigRenderOptions
import net.shrine.log.Loggable
import net.shrine.source.ConfigSource
import spray.http.{StatusCode, StatusCodes}
import spray.routing.{HttpService, _}

import scala.util.Try

/**
  * Created by ty on 11/8/16.
  */
trait MetaDataService extends HttpService with Loggable {
  lazy val config = ConfigSource.config.getConfig("shrine.metaData")
  val info =
    """
      |The SHRINE Metadata service. This is a simple API that gives you
      |read access to the metaData section within SHRINE's configuration.
      |You can access this data by key, or by accessing the entire metaData
      |config section at once. To access everything at once, make a GET
      |to shrine-metadata/data (if on a browser, just add /data to the
      |end of the current url). To access values by key, make a GET to
      |shrine-metadata/data?key={{your key here without braces}} (again,
      |if on a browser just add /data?key={{your key}} to the end of the url).
    """.stripMargin

  lazy val route: Route = get {
    pathPrefix("ping") { complete("pong")} ~
    pathPrefix("data") {
      parameter("key") { (key: String) =>
        complete(handleKey(key))
      } ~ complete(handleAll)
    }} ~ complete(info)

  def handleAll:(StatusCode, String) = {
    StatusCodes.OK -> config.root.render(ConfigRenderOptions.concise())
  }

  def handleKey(key: String): (StatusCode, String) = {
    Try(StatusCodes.OK -> config.getValue(key).render(ConfigRenderOptions.concise()))
      .getOrElse(StatusCodes.NotFound ->
        s"Could not find a value for the specified path `$key`")
  }
}
