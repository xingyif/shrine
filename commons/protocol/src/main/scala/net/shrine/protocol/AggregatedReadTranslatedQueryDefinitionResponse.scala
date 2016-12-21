package net.shrine.protocol

import AggregatedReadTranslatedQueryDefinitionResponse.rootTagName

/**
  * @author clint
  * @date Feb 13, 2014
  */
final case class AggregatedReadTranslatedQueryDefinitionResponse(translated: Seq[SingleNodeTranslationResult]) extends AbstractReadTranslatedQueryDefinitionResponse(rootTagName)

object AggregatedReadTranslatedQueryDefinitionResponse extends AbstractReadTranslatedQueryDefinitionResponse.Companion(new AggregatedReadTranslatedQueryDefinitionResponse(_)) {
  override val rootTagName = "aggregatedReadTranslatedQueryDefinitionResponse"
}