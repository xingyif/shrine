package net.shrine.problem

import com.typesafe.config.{ConfigValue, ConfigValueFactory}
import net.shrine.config.ConfigSource

/**
  * Source of typesafe config for the problems database
  *
  * @author ty
  * @since 7/22/16
  */
object ProblemConfigSource extends ConfigSource {
  override val configName: String = "problem"
  var turnOffConnector = false
  override def config = {
    atomicConfig.config.withValue("turnOffConnector", ConfigValueFactory.fromAnyRef(turnOffConnector))
  }
}
