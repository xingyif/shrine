package net.shrine.broadcaster

import net.shrine.protocol.BroadcastMessage
import scala.concurrent.Future
import net.shrine.protocol.SingleNodeResult

/**
 * @author clint
 * @date Feb 28, 2014
 */
trait BroadcasterClient {
  def broadcast(message: BroadcastMessage): Future[Iterable[SingleNodeResult]]
}