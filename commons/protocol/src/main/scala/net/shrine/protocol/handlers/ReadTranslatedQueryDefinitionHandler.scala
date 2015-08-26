package net.shrine.protocol.handlers

import net.shrine.protocol.ReadTranslatedQueryDefinitionRequest
import net.shrine.protocol.NonI2b2ShrineResponse
import net.shrine.protocol.BaseShrineResponse

/**
 * @author clint
 * @date Feb 14, 2014
 */
trait ReadTranslatedQueryDefinitionHandler[Req, Resp] {
  def readTranslatedQueryDefinition(request: Req, shouldBroadcast: Boolean = true): Resp
}