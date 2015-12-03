package net.shrine.wiring

import net.shrine.adapter.client.RemoteAdapterClient
import net.shrine.broadcaster.{IdAndUrl, NodeHandle}
import net.shrine.crypto.TrustParam
import scala.concurrent.duration.Duration
import net.shrine.adapter.service.AdapterRequestHandler
import net.shrine.adapter.client.InJvmAdapterClient
import net.shrine.protocol.NodeId
import net.shrine.client.JerseyHttpClient
import javax.ws.rs.core.MediaType
import net.shrine.client.Poster
import net.shrine.protocol.ResultOutputType

/**
 * @author clint
 * @since Dec 11, 2013
 */
object NodeHandleSource {
  
  //TODO: Allow per-node timeouts?
  def makeNodeHandles(
      trustParam: TrustParam, 
      timeout: Duration, 
      nodes: Iterable[IdAndUrl], 
      idOfThisNode: NodeId,
      adapterServiceOption: Option[AdapterRequestHandler],
      breakdownTypes: Set[ResultOutputType]): Set[NodeHandle] = {
    
    val httpClient = new JerseyHttpClient(trustParam, timeout, MediaType.APPLICATION_XML)
    
    val downstream = nodes.map { case IdAndUrl(nodeId, url) =>
      val poster = Poster(url.toString, httpClient)
      
      NodeHandle(nodeId, RemoteAdapterClient(nodeId, poster, breakdownTypes))
    }.toSet
    
    val handleToThisNodeOption: Option[NodeHandle] = {
      adapterServiceOption.map { adapterService =>
        NodeHandle(idOfThisNode, new InJvmAdapterClient(adapterService))
      }
    }
    
    downstream ++ handleToThisNodeOption
  }
}