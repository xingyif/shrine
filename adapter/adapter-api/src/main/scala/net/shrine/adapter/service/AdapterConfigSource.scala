package net.shrine.adapter.service

import com.typesafe.config.{Config, ConfigFactory}
import net.shrine.config.AtomicConfigSource

/**
 * Source of config for the Adapter. Put new config fields here, not in AdapterConfig, to enable Config-based apply() methods.
 *
 * @author david
 * @since 8/25/15
 */
object AdapterConfigSource {

  val atomicConfig = new AtomicConfigSource(ConfigFactory.load("shrine"))

  def config:Config = {
    atomicConfig.config
  }

  def configForBlock[T](key:String,value:AnyRef,origin:String)(block: => T):T = {
    atomicConfig.configForBlock(key,value,origin)(block)
  }

  //todo move this to common code somewhere
  def objectForName[T](objectName:String):T = {

    import scala.reflect.runtime.universe
    val runtimeMirror = universe.runtimeMirror(getClass.getClassLoader)
    val module = runtimeMirror.staticModule(objectName)

    val reflectedObj = runtimeMirror.reflectModule(module)
    val obj = reflectedObj.instance

    obj.asInstanceOf[T]
  }
}