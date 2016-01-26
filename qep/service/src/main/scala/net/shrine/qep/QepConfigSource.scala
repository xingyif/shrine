package net.shrine.qep

import com.typesafe.config.{Config, ConfigFactory}
import net.shrine.config.AtomicConfigSource
import net.shrine.log.Log

/**
 * Source of config for the Qep. Put new config fields here, not in QepConfig, to enable Config-based apply() methods.
 *
 * @author david 
 * @since 8/18/15
 */
object QepConfigSource {

  val atomicConfig = new AtomicConfigSource(ConfigFactory.load("shrine"))

  def config:Config = {
    atomicConfig.config
  }

  Log.debug(s"shrine.queryEntryPoint.audit.collectQepAudit is ${config.getBoolean("shrine.queryEntryPoint.audit.collectQepAudit")}")

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