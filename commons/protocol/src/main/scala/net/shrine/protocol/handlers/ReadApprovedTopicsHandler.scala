package net.shrine.protocol.handlers

import net.shrine.protocol.ShrineResponse
import net.shrine.protocol.ReadApprovedQueryTopicsRequest
import net.shrine.protocol.BaseShrineResponse

/**
 * @author clint
 * @date Mar 29, 2013
 */
trait ReadApprovedTopicsHandler[Req, Resp] {
  def readApprovedQueryTopics(request: Req, shouldBroadcast: Boolean = true): Resp
}