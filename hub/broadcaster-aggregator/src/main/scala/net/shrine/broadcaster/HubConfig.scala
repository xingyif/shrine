package net.shrine.broadcaster

import com.typesafe.config.Config
import net.shrine.config.{DurationConfigParser,ConfigExtensions,Keys}

import scala.concurrent.duration.Duration

/**
 * @author clint
 * @since Jan 17, 2014
 */
final case class HubConfig(
  maxQueryWaitTime: Duration,
  downstreamNodes: Iterable[IdAndUrl],
  shouldQuerySelf: Boolean)

object HubConfig {

  val defaultShouldQuerySelf = false
  
  def apply(config: Config): HubConfig = {
    import Keys._

    HubConfig(
      config.getConfigured(maxQueryWaitTime,DurationConfigParser(_)),
      config.getOptionConfigured(downstreamNodes, NodeListParser(_)).getOrElse(Nil),
      config.getOption(shouldQuerySelf, _.getBoolean).getOrElse(defaultShouldQuerySelf))
  }
}