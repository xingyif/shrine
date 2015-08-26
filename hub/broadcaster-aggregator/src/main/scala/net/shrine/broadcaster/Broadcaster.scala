package net.shrine.broadcaster

import net.shrine.protocol.BroadcastMessage

/**
 * @author clint
 * @date Nov 15, 2013
 */
trait Broadcaster {
  def broadcast(message: BroadcastMessage): Multiplexer
}