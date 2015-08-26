package net.shrine.protocol.handlers

import net.shrine.protocol.RunQueryRequest
import net.shrine.protocol.ShrineResponse
import net.shrine.protocol.BaseShrineResponse

/**
 * @author clint
 * @date Mar 29, 2013
 */
trait RunQueryHandler[Req, Resp] {
  def runQuery(request: Req, shouldBroadcast: Boolean = true): Resp
}