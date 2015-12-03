package net.shrine.broadcaster.service

import net.shrine.protocol.BroadcastMessage
import net.shrine.protocol.SingleNodeResult

/**
 * @author clint
 * @date Feb 28, 2014
 */
trait BroadcasterMultiplexerRequestHandler {
  def broadcastAndMultiplex(message: BroadcastMessage): Iterable[SingleNodeResult]
}