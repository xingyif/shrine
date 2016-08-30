package net.shrine.problem

import com.typesafe.config.{Config, ConfigValue, ConfigValueFactory}
import net.shrine.source.ConfigSource

/**
  * Source of typesafe config for the problems database
  *
  * @author ty
  * @since 7/22/16
  */
object ProblemConfigSource extends ConfigSource {
  override val configName: String = "dashboard"

  // Makes it so constructing a problem in this context won't log it to the connector
  // Does not stop you from constructing the connector and using it manually
  var turnOffConnector = false

  def get[T](path: String, config:Config):T = {
    objectForName(config.getString(path))
  }
}
