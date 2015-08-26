package net.shrine.config

import scala.concurrent.duration.Duration

import com.typesafe.config.Config

/**
 * @author clint
 * @since Dec 5, 2013
 */
object DurationConfigParser {
  object Keys {
    val milliseconds = "milliseconds"
    val seconds = "seconds"
    val minutes = "minutes"
    val hours = "hours"
    val days = "days"
    val inf = "inf"
  }

  def apply(subConfig: Config): Duration = {
    import scala.concurrent.duration._
    import Keys._

    require(subConfig.entrySet.size == 1, s"Only one time unit is allowed, but got $subConfig")
    
    if (subConfig.hasPath(milliseconds)) { subConfig.getDouble(milliseconds).milliseconds }
    else if (subConfig.hasPath(seconds)) { subConfig.getDouble(seconds).seconds }
    else if (subConfig.hasPath(minutes)) { subConfig.getDouble(minutes).minutes }
    else if (subConfig.hasPath(hours)) { subConfig.getDouble(hours).hours }
    else if (subConfig.hasPath(days)) { subConfig.getDouble(days).days }
    else { throw new Exception(s"Expected to find one of '$milliseconds', '$seconds', '$minutes', '$hours' or '$days' at subConfig $subConfig") }
  }
}