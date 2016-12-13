package net.shrine.source

import com.typesafe.config.{Config, ConfigException, ConfigFactory, ConfigValueFactory}
import net.shrine.log.Log

import scala.util.control.NonFatal

/**
  * @author ty
  * @since  7/22/16
  */
object ConfigSource {

  val configName: String = "shrine"

  lazy val atomicConfig = new AtomicConfigSource(ConfigFactory.load(configName))

  def config: Config = {
    try atomicConfig.config
    catch {
      case NonFatal(x) =>
        Log.error(s"Could not load config file $configName.conf due to ",x)
        throw x
    }
  }
  def configForBlock[T](key: String, value: AnyRef, origin: String)(block: => T): T = {
    atomicConfig.configForBlock(key, value, origin)(block)
  }

  def configForBlock[T](config:Config,origin:String)(block: => T):T = {
    atomicConfig.configForBlock(config,origin)(block)
  }

  def objectForName[T](objectName: String): T = {

    import scala.reflect.runtime.universe
    val runtimeMirror = universe.runtimeMirror(getClass.getClassLoader)
    val module = runtimeMirror.staticModule(objectName)

    val reflectedObj = runtimeMirror.reflectModule(module)
    val obj = reflectedObj.instance

    obj.asInstanceOf[T]
  }

  def getObject[T](path: String, config:Config):T = {
    try {
      objectForName(config.getString(path))
    } catch {
      case cx:ConfigException => throw ConfigError(cx, path)
    }
  }
}

case class ConfigError(throwable: Throwable, path: String) extends Error {
  override def getMessage:String = s"Malformed config file, could not retrieve path '$path'"
}