package net.shrine.steward

import com.typesafe.config.{Config, ConfigFactory}
import net.shrine.authorization.steward.TopicState
import net.shrine.config.AtomicConfigSource

/**
 * Source of typesafe config for the data steward app.
 *
 * @author david 
 * @since 4/29/15
 */

object StewardConfigSource {

  val atomicConfig = new AtomicConfigSource(ConfigFactory.load("steward"))

  def config:Config = {
    atomicConfig.config
  }

  def configForBlock[T](key:String,value:AnyRef,origin:String)(block: => T):T = {
    atomicConfig.configForBlock(key,value,origin)(block)
  }

  val createTopicsModeConfigKey = "shrine.steward.createTopicsMode"
  def createTopicsInState:CreateTopicsMode = CreateTopicsMode.namesToCreateTopicsMode(config.getString(createTopicsModeConfigKey))

  def objectForName[T](objectName:String):T = {

    import scala.reflect.runtime.universe
    val runtimeMirror = universe.runtimeMirror(getClass.getClassLoader)
    val module = runtimeMirror.staticModule(objectName)

    val reflectedObj = runtimeMirror.reflectModule(module)
    val obj = reflectedObj.instance

    obj.asInstanceOf[T]
  }
}

sealed case class CreateTopicsMode(name:String,topicState: TopicState)

object CreateTopicsMode{
  val Pending = CreateTopicsMode(TopicState.pending)
  val Approved = CreateTopicsMode(TopicState.approved)
  val TopicsIgnoredJustLog = CreateTopicsMode("TopicsIgnoredJustLog",TopicState.approved)
  
  val all = Set(Pending,Approved,TopicsIgnoredJustLog)
  val namesToCreateTopicsMode: Map[String, CreateTopicsMode] = all.map(x => (x.name,x)).toMap
  
  def apply(topicState: TopicState):CreateTopicsMode = CreateTopicsMode(topicState.name,topicState)
}