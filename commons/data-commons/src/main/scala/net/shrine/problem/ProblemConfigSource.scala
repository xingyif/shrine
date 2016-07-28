package net.shrine.problem

import net.shrine.config.ConfigSource

/**
  * Source of typesafe config for the problems database
  *
  * @author ty
  * @since 7/22/16
  */
object ProblemConfigSource extends ConfigSource {
  override val configName: String = "problem"
}
