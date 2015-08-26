package net.shrine.broadcaster

import net.shrine.protocol.Result
import net.shrine.adapter.client.AdapterClient
import net.shrine.protocol.BroadcastMessage
import scala.concurrent.Future

/**
 * @author clint
 * @date Dec 3, 2013
 */
final case class MockAdapterClient(toReturn: Result) extends AdapterClient {
  var messageParam: BroadcastMessage = _

  override def query(message: BroadcastMessage): Future[Result] = {
    messageParam = message

    Future.successful(toReturn)
  }
}
