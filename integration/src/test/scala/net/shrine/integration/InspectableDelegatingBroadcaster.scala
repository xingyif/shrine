package net.shrine.integration

import net.shrine.broadcaster.Broadcaster
import net.shrine.protocol.BroadcastMessage
import net.shrine.broadcaster.Multiplexer
import net.shrine.broadcaster.BufferingMultiplexer

/**
 * @author clint
 * @date Jan 8, 2014
 */
final case class InspectableDelegatingBroadcaster(delegate: Broadcaster) extends Broadcaster {
  private[this] val lock = new AnyRef
  
  @volatile private[this] var lastMultiplexerOption: Option[BufferingMultiplexer] = None
  
  def lastMultiplexer = lock.synchronized { lastMultiplexerOption }
  
  override def broadcast(message: BroadcastMessage): Multiplexer = {
    val result = delegate.broadcast(message)
    
    lock.synchronized {
      lastMultiplexerOption = Option(result.asInstanceOf[BufferingMultiplexer])
    }
    
    result
  }
}