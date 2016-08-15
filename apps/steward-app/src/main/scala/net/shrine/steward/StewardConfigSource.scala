package net.shrine.steward

import net.shrine.authorization.steward.TopicState
import net.shrine.source.ConfigSource

/**
  * Source of typesafe config for the data steward app.
  *
  * @author david
  * @since 4/29/15
  */

object StewardConfigSource extends ConfigSource {
  override val configName = "steward"
  val createTopicsModeConfigKey = "shrine.steward.createTopicsMode"
  def createTopicsInState:CreateTopicsMode = CreateTopicsMode.namesToCreateTopicsMode(config.getString(createTopicsModeConfigKey))

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