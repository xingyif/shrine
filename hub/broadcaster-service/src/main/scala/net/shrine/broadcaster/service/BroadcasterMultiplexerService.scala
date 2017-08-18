package net.shrine.broadcaster.service

import net.shrine.protocol.BroadcastMessage
import scala.concurrent.duration.Duration
import net.shrine.broadcaster.Broadcaster
import net.shrine.protocol.SingleNodeResult
import scala.concurrent.Await

/**
 * @author clint
 * @since Feb 28, 2014
 */
//todo this class looks pretty thin and very suspicious
final case class BroadcasterMultiplexerService(broadcaster: Broadcaster, maxQueryWaitTime: Duration) extends BroadcasterMultiplexerRequestHandler {
  override def broadcastAndMultiplex(message: BroadcastMessage): Iterable[SingleNodeResult] = {
    val multiplexer = broadcaster.broadcast(message)

    //todo here's one end of a race condition waiting on a response from the Adapter.
    Await.result(multiplexer.responses, maxQueryWaitTime)
  }
}