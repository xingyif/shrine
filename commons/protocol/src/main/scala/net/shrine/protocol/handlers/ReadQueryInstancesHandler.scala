package net.shrine.protocol.handlers

import net.shrine.protocol.ReadQueryInstancesRequest
import net.shrine.protocol.ShrineResponse
import net.shrine.protocol.BaseShrineResponse

/**
 * @author clint
 * @date Mar 29, 2013
 */
trait ReadQueryInstancesHandler[Req, Resp] {
  def readQueryInstances(request: Req, shouldBroadcast: Boolean = true): Resp
}