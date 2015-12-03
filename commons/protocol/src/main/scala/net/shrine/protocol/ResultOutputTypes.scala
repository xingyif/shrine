package net.shrine.protocol

import com.typesafe.config.Config

import net.shrine.config.ConfigExtensions

/**
 * @author clint
 * @since Oct 24, 2014
 */
object ResultOutputTypes {
  def fromConfig(config: Config): Set[ResultOutputType] = {

    def parseResultOutputType(name: String, config: Config): ResultOutputType = {
      val description = config.getString("description")
      val displayType = config.getOption("displayType",_.getString).getOrElse(ResultOutputType.defaultDisplayType)

      ResultOutputType(name, isBreakdown = true, ResultOutputType.I2b2Options(description, displayType), None)
    }

    import scala.collection.JavaConverters._

    config.root.keySet.asScala.toSet.map { name: String =>
      parseResultOutputType(name, config.getConfig(name))
    }
  }
}