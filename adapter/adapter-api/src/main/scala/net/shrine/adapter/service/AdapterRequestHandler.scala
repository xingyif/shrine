package net.shrine.adapter.service

import net.shrine.protocol.BroadcastMessage
import net.shrine.protocol.Result

/**
 * @author clint
 * @date Nov 14, 2013
 */
trait AdapterRequestHandler {
  def handleRequest(request: BroadcastMessage): Result
}