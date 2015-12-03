package net.shrine.dashboard

import com.typesafe.config.{Config, ConfigFactory}
import net.shrine.config.AtomicConfigSource

/**
 * Source of typesafe config for the dashboard app.
 *
 * @author david 
 * @since 4/29/15
 */

object DashboardConfigSource {

  val atomicConfig = new AtomicConfigSource(ConfigFactory.load("dashboard"))

  def config:Config = {
    atomicConfig.config
  }

  def configForBlock[T](key:String,value:AnyRef,origin:String)(block: => T):T = {
    atomicConfig.configForBlock(key,value,origin)(block)
  }

}
