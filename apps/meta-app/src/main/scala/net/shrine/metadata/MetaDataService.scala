package net.shrine.metadata

import com.typesafe.config.ConfigRenderOptions
import net.shrine.log.Loggable
import spray.http.{StatusCode, StatusCodes}
import spray.routing.{HttpService, _}

import scala.util.Try

/**
  * Created by ty on 11/8/16.
  */
trait MetaDataService extends HttpService with Loggable {
  lazy val config = MetaConfigSource.config.getConfig("shrine.metaData")

  lazy val route: Route = get {
    pathPrefix("ping") { complete("pong")} ~
    pathPrefix("data") {
      parameter("key") { (key: String) =>
        complete(handleKey(key))
      } ~ complete(handleAll)
    }}

  def handleAll:(StatusCode, String) = {
    StatusCodes.OK -> config.root.render(ConfigRenderOptions.concise())
  }

  def handleKey(key: String): (StatusCode, String) = {
    Try(StatusCodes.OK -> config.getValue(key).render(ConfigRenderOptions.concise()))
      .getOrElse(StatusCodes.NotFound ->
        s"Could not find a value for the specified path `$key`")
  }
}
