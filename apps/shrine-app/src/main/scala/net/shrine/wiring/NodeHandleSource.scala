package net.shrine.wiring

import com.typesafe.config.Config
import net.shrine.adapter.client.RemoteAdapterClient
import net.shrine.broadcaster.{NodeListParser, IdAndUrl, NodeHandle}
import net.shrine.config.DurationConfigParser
import net.shrine.config.Keys._
import net.shrine.crypto.TrustParam
import scala.concurrent.duration.Duration
import net.shrine.adapter.service.AdapterRequestHandler
import net.shrine.adapter.client.InJvmAdapterClient
import net.shrine.protocol.NodeId
import net.shrine.client.JerseyHttpClient
import javax.ws.rs.core.MediaType
import net.shrine.client.Poster
import net.shrine.protocol.ResultOutputType
import net.shrine.config.{DurationConfigParser,ConfigExtensions}

/**
 * @author clint
 * @since Dec 11, 2013
 */
object NodeHandleSource {
  
  //TODO: Allow per-node timeouts?
  //todo move all this to the NodeHandle's apply method
  def makeNodeHandles(
      hubConfig:Config,
      trustParam: TrustParam, 
      idOfThisNode: NodeId,
      adapterServiceOption: Option[AdapterRequestHandler],
      breakdownTypes: Set[ResultOutputType]): Set[NodeHandle] = {

    val timeout = hubConfig.getConfigured("maxQueryWaitTime",DurationConfigParser(_))
    val httpClient = new JerseyHttpClient(trustParam, timeout, MediaType.APPLICATION_XML)

    val nodes = hubConfig.getOptionConfigured("downstreamNodes", NodeListParser(_)).getOrElse(Nil)
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