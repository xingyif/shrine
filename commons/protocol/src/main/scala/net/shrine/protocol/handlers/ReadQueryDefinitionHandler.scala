package net.shrine.protocol.handlers

import net.shrine.protocol.ShrineResponse
import net.shrine.protocol.ReadQueryDefinitionRequest
import net.shrine.protocol.BaseShrineResponse
import net.shrine.protocol.AbstractReadQueryDefinitionRequest

/**
 * @author clint
 * @date Mar 29, 2013
 */
trait ReadQueryDefinitionHandler[Req <: AbstractReadQueryDefinitionRequest, Resp <: BaseShrineResponse] {
  def readQueryDefinition(request: Req, shouldBroadcast: Boolean = true): Resp
}