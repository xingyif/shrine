package net.shrine.integration

import java.util.concurrent.LinkedBlockingDeque

import net.shrine.broadcaster.{Broadcaster, BufferingMultiplexer, Multiplexer, NodeHandle}
import net.shrine.protocol.BroadcastMessage

import scala.concurrent.duration.FiniteDuration

/**
 * @author clint
 * @since Jan 8, 2014
 */
final case class InspectableDelegatingBroadcaster(delegate: Broadcaster) extends Broadcaster {

  /*used to provide a LIFO queue to examine multiplexers*/
  private val multiplexersDelegatedTo = new LinkedBlockingDeque[BufferingMultiplexer]()

  def pollMultiplexerLifo(timeout:FiniteDuration): Option[BufferingMultiplexer] = Option(multiplexersDelegatedTo.pollFirst(timeout.length,timeout.unit))

  override def broadcast(message: BroadcastMessage): Multiplexer = {
    val result = delegate.broadcast(message)
    multiplexersDelegatedTo.put(result.asInstanceOf[BufferingMultiplexer])

    result
  }

  override def destinations: Set[NodeHandle] = ???
}