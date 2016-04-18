package net.shrine.qep

import com.typesafe.config.Config
import net.shrine.config.{DurationConfigParser, Keys}
import net.shrine.log.Log

import scala.concurrent.duration.Duration

/**
 * @author clint
 * @since Feb 28, 2014
 */
final case class QepConfig (
  includeAggregateResults: Boolean,
  maxQueryWaitTime: Duration,
  collectQepAudit:Boolean) {

  Log.debug(s"QepConfig collectQepAudit is $collectQepAudit")

}

object QepConfig {

  def apply(config: Config): QepConfig = {
    import Keys._

    QepConfig(
      config.getBoolean(includeAggregateResults),
      DurationConfigParser(config.getConfig("maxQueryWaitTime")),
    //todo change to shrine.queryEntryPoint...
      QepConfigSource.config.getBoolean("shrine.queryEntryPoint.audit.collectQepAudit")
    )
  }
}
