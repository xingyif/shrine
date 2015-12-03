package net.shrine.protocol.handlers

import net.shrine.protocol.RenameQueryRequest
import net.shrine.protocol.ShrineResponse
import net.shrine.protocol.BaseShrineResponse

/**
 * @author clint
 * @date Mar 29, 2013
 */
trait RenameQueryHandler[Req, Resp] {
  def renameQuery(request: Req, shouldBroadcast: Boolean = true): Resp
}