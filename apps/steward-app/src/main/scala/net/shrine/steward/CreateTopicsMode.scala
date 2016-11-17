package net.shrine.steward

import net.shrine.authorization.steward.TopicState
import net.shrine.source.ConfigSource

/**
  * Source of typesafe config for the data steward app.
  *
  * @author david
  * @since 4/29/15
  */
sealed case class CreateTopicsMode(name:String,topicState: TopicState)

object CreateTopicsMode{
  val createTopicsModeConfigKey = "shrine.steward.createTopicsMode"

  def createTopicsInState:CreateTopicsMode = all.map(x => (x.name,x)).toMap.apply(ConfigSource.config.getString(createTopicsModeConfigKey))

  val Pending = CreateTopicsMode(TopicState.pending)
  val Approved = CreateTopicsMode(TopicState.approved)
  val TopicsIgnoredJustLog = CreateTopicsMode("TopicsIgnoredJustLog",TopicState.approved)
  
  val all = Set(Pending,Approved,TopicsIgnoredJustLog)

  def apply(topicState: TopicState):CreateTopicsMode = CreateTopicsMode(topicState.name,topicState)
}