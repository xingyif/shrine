package net.shrine.broadcaster.service

import net.shrine.protocol.BroadcastMessage
import scala.concurrent.duration.Duration
import net.shrine.broadcaster.BroadcasterClient
import net.shrine.protocol.SingleNodeResult
import scala.concurrent.Await
import net.shrine.broadcaster.Broadcaster

/**
 * @author clint
 * @date Feb 28, 2014
 */
final case class BroadcasterMultiplexerService(broadcaster: Broadcaster, maxQueryWaitTime: Duration) extends BroadcasterMultiplexerRequestHandler {
  override def broadcastAndMultiplex(message: BroadcastMessage): Iterable[SingleNodeResult] = {
    val multiplexer = broadcaster.broadcast(message)
    
    Await.result(multiplexer.responses, maxQueryWaitTime)
  }
}