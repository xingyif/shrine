package net.shrine.protocol.handlers

import net.shrine.protocol.ReadQueryResultRequest
import net.shrine.protocol.ShrineResponse
import net.shrine.protocol.BaseShrineResponse

/**
 * @author clint
 * @date Mar 29, 2013
 */
trait ReadQueryResultHandler[Req, Resp] {
  def readQueryResult(request: Req, shouldBroadcast: Boolean = true): Resp
}