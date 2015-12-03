package net.shrine.protocol.handlers

import net.shrine.protocol.ReadPdoRequest
import net.shrine.protocol.ShrineResponse
import net.shrine.protocol.BaseShrineResponse

/**
 * @author clint
 * @date Mar 29, 2013
 */
trait ReadPdoHandler[Req, Resp] {
  def readPdo(request: Req, shouldBroadcast: Boolean = true): Resp
}