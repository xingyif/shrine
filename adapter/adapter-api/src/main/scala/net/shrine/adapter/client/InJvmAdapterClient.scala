package net.shrine.adapter.client

import java.net.URL

import net.shrine.protocol.BroadcastMessage
import scala.concurrent.Future
import net.shrine.protocol.Result
import net.shrine.adapter.service.AdapterRequestHandler

/**
 * @author clint
 * @date Jan 7, 2014
 */

//todo I think this is only used in shouldQuerySelf, not in the mocks!
final case class InJvmAdapterClient(adapterService: AdapterRequestHandler) extends AdapterClient {
  override def query(message: BroadcastMessage): Future[Result] = {
    //TODO: REVISIT THIS!
    import scala.concurrent.ExecutionContext.Implicits.global
    
    Future(adapterService.handleRequest(message))
  }

  //todo when you replace this class with loopback, change this to be just a raw URL
  def url:Option[URL] = None

}