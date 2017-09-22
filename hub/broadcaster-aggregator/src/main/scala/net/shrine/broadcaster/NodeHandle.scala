package net.shrine.broadcaster

import javax.ws.rs.core.MediaType

import com.typesafe.config.Config
import net.shrine.adapter.client.{AdapterClient, InJvmAdapterClient, RemoteAdapterClient}
import net.shrine.adapter.service.AdapterRequestHandler
import net.shrine.client.{JerseyHttpClient, Poster}
import net.shrine.config.{ConfigExtensions, DurationConfigParser}
import net.shrine.crypto.TrustParam
import net.shrine.protocol.{NodeId, ResultOutputType}

/**
 * @author clint
 * @since Nov 15, 2013
 */
final case class NodeHandle(nodeId: NodeId, client: AdapterClient)

/**
  * @author clint
  * @since Dec 11, 2013
  */
object NodeHandle {

  def makeNodeHandles(
                       hubConfig:Config,
                       trustParam: TrustParam,
                       idOfThisNode: NodeId,
                       adapterServiceOption: Option[AdapterRequestHandler],
                       breakdownTypes: Set[ResultOutputType]): Set[NodeHandle] = {

    val timeout = hubConfig.getConfigured("maxQueryWaitTime",DurationConfigParser(_))
    val httpClient = new JerseyHttpClient(trustParam, timeout, MediaType.APPLICATION_XML)

    val nodes: Iterable[IdAndUrl] = hubConfig.getOptionConfigured("downstreamNodes", NodeListParser(_)).getOrElse(Nil)
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