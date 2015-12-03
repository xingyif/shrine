package net.shrine.adapter.client

import net.shrine.protocol.BroadcastMessage
import scala.concurrent.Future
import net.shrine.protocol.Result
import net.shrine.adapter.service.AdapterRequestHandler

/**
 * @author clint
 * @date Jan 7, 2014
 */
final case class InJvmAdapterClient(adapterService: AdapterRequestHandler) extends AdapterClient {
  override def query(message: BroadcastMessage): Future[Result] = {
    //TODO: REVISIT THIS!
    import scala.concurrent.ExecutionContext.Implicits.global
    
    Future(adapterService.handleRequest(message))
  }
}