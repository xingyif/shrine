package net.shrine.adapter.client

import java.net.URL

import net.shrine.protocol.BroadcastMessage
import net.shrine.protocol.Result
import scala.concurrent.Future

/**
 * @author clint
 * @since Nov 15, 2013
 */
trait AdapterClient {
  def query(message: BroadcastMessage): Future[Result]

  def url:Option[URL]
}