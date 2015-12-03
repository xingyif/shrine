package net.shrine.protocol.handlers

import net.shrine.protocol.ReadInstanceResultsRequest
import net.shrine.protocol.ShrineResponse
import net.shrine.protocol.BaseShrineResponse

/**
 * @author clint
 * @date Mar 29, 2013
 */
trait ReadInstanceResultsHandler[Req, Resp] {
  def readInstanceResults(request: Req, shouldBroadcast: Boolean = true): Resp
}