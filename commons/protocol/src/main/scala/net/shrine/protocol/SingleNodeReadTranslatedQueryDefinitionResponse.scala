package net.shrine.protocol

import SingleNodeReadTranslatedQueryDefinitionResponse.rootTagName

/**
 * @author clint
 * @date Feb 13, 2014
 */
final case class SingleNodeReadTranslatedQueryDefinitionResponse(result: SingleNodeTranslationResult) extends AbstractReadTranslatedQueryDefinitionResponse(rootTagName) {
  override val translated: Seq[SingleNodeTranslationResult] = Seq(result)
}

object SingleNodeReadTranslatedQueryDefinitionResponse extends AbstractReadTranslatedQueryDefinitionResponse.Companion(results => new SingleNodeReadTranslatedQueryDefinitionResponse(results.head)) {
  override val rootTagName = "singleNodeReadTranslatedQueryDefinitionResponse"
}