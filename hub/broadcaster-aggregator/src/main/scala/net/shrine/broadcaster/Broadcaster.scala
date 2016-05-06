package net.shrine.broadcaster

import net.shrine.protocol.BroadcastMessage

/**
 * @author clint
 * @since Nov 15, 2013
 */
//todo this trait exists only to be mocked.
trait Broadcaster {
  def broadcast(message: BroadcastMessage): Multiplexer

  def destinations: Set[NodeHandle]
}