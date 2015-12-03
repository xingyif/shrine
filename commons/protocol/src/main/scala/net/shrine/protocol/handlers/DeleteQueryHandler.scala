package net.shrine.protocol.handlers

import net.shrine.protocol.DeleteQueryRequest
import net.shrine.protocol.ShrineResponse
import net.shrine.protocol.BaseShrineResponse

/**
 * @author clint
 * @date Mar 29, 2013
 */
trait DeleteQueryHandler[Req, Resp] {
  def deleteQuery(request: Req, shouldBroadcast: Boolean = true): Resp
}