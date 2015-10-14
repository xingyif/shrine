package net.shrine.dashboard

import java.util.concurrent.atomic.AtomicReference

import com.typesafe.config.{Config, ConfigFactory}
import net.shrine.config.AtomicConfigSource

/**
 * Source of typesafe config for the data steward app.
 *
 * @author david 
 * @since 4/29/15
 */

object AdminConfigSource {

  val atomicConfig = new AtomicConfigSource(ConfigFactory.load("admin"))

  def config:Config = {
    atomicConfig.config
  }

  def configForBlock[T](key:String,value:AnyRef,origin:String)(block: => T):T = {
    atomicConfig.configForBlock(key,value,origin)(block)
  }

}
