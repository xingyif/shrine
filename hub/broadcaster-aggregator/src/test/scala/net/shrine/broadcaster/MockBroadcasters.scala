package net.shrine.broadcaster

import net.shrine.protocol.BroadcastMessage
import net.shrine.protocol.SingleNodeResult
import net.shrine.protocol.NodeId

/**
 * @author clint
 * @since Feb 28, 2014
 */
object MockBroadcasters {
  final case class MockAdapterClientBroadcaster(toReturn: Map[NodeId, SingleNodeResult]) extends Broadcaster {
    var messageParam: BroadcastMessage = _

    def broadcast(message: BroadcastMessage): Multiplexer = {
      messageParam = message

      val multiplexer: Multiplexer = new BufferingMultiplexer(toReturn.keySet)

      for {
        (nodeId, result) <- toReturn
      } {
        multiplexer.processResponse(result)
      }

      multiplexer
    }

    override def destinations: Set[NodeHandle] = ???
  }
}