package net.shrine.adapter

import net.shrine.protocol.BroadcastMessage
import net.shrine.adapter.translators.QueryDefinitionTranslator
import net.shrine.protocol.ReadTranslatedQueryDefinitionRequest
import net.shrine.protocol.BaseShrineResponse
import net.shrine.protocol.SingleNodeReadTranslatedQueryDefinitionResponse
import net.shrine.protocol.NodeId
import net.shrine.protocol.SingleNodeTranslationResult

/**
 * @author clint
 * @date Feb 13, 2014
 */
final class ReadTranslatedQueryDefinitionAdapter(enclosingNodeId: NodeId, translator: QueryDefinitionTranslator) extends Adapter {
  override protected[adapter] def processRequest(message: BroadcastMessage): BaseShrineResponse = {
    val req = message.request.asInstanceOf[ReadTranslatedQueryDefinitionRequest]
    
    val translated = translator.translate(req.queryDef)
    
    SingleNodeReadTranslatedQueryDefinitionResponse(SingleNodeTranslationResult(enclosingNodeId, translated))
  }
}