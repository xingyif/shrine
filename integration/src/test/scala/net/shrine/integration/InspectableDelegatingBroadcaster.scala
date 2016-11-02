package net.shrine.integration

import net.shrine.broadcaster.{NodeHandle, Broadcaster, Multiplexer, BufferingMultiplexer}
import net.shrine.protocol.BroadcastMessage

/**
 * @author clint
 * @since Jan 8, 2014
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

  override def destinations: Set[NodeHandle] = ???
}