package net.shrine.protocol.handlers

import net.shrine.protocol.ShrineResponse
import net.shrine.protocol.RunHeldQueryRequest

/**
 * @author clint
 * @date Apr 30, 2014
 */
trait RunHeldQueryHandler {
  def runHeldQuery(request: RunHeldQueryRequest, shouldBroadcast: Boolean): ShrineResponse
}