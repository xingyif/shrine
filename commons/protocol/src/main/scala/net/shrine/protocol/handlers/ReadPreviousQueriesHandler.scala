package net.shrine.protocol.handlers

import net.shrine.protocol.ReadPreviousQueriesRequest
import net.shrine.protocol.ShrineResponse
import net.shrine.protocol.BaseShrineResponse

/**
 * @author clint
 * @date Mar 29, 2013
 */
trait ReadPreviousQueriesHandler[Req, Resp] {
  def readPreviousQueries(request: Req, shouldBroadcast: Boolean = true): Resp
}