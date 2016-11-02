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
  override val configName: String = "shrine"

  def getObject[T](path: String, config:Config):T = {
    objectForName(config.getString(path))
  }
}
