import akka.actor.{Actor, ActorRefFactory}
import com.typesafe.config.ConfigRenderOptions
import net.shrine.log.Loggable
import net.shrine.source.ConfigSource
import spray.http.{StatusCode, StatusCodes}
import spray.routing.{HttpService, Route}

import scala.util.Try

/**
  * A super simple API that provides access to the MetaData section of SHRINE's configuration
  */

class MetaDataActor extends Actor with MetaDataService {
  override def receive: Receive = runRoute(route)

  override def actorRefFactory: ActorRefFactory = context
}

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

object MetaConfigSource extends ConfigSource