package net.shrine.adapter.client

import net.shrine.protocol.BroadcastMessage
import net.shrine.protocol.Result
import scala.concurrent.Future

/**
 * @author clint
 * @date Nov 15, 2013
 */
trait AdapterClient {
  def query(message: BroadcastMessage): Future[Result]
}