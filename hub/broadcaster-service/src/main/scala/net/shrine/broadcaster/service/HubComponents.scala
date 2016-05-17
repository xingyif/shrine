package net.shrine.broadcaster.service

import com.typesafe.config.Config
import net.shrine.adapter.service.AdapterRequestHandler
import net.shrine.broadcaster.dao.HubDao
import net.shrine.broadcaster.{AdapterClientBroadcaster, NodeHandle, NodeHandleSource}
import net.shrine.config.{ConfigExtensions, DurationConfigParser}
import net.shrine.crypto.TrustParam
import net.shrine.protocol.{NodeId, ResultOutputType}

/**
  * @author david 
  * @since 1.22
  */
case class HubComponents(broadcastDestinations: Set[NodeHandle],broadcasterMultiplexerService: BroadcasterMultiplexerService,broadcasterMultiplexerResource:BroadcasterMultiplexerResource)

object HubComponents {

  def apply(
             hubConfig:Config,
             keystoreTrustParam:TrustParam,
             nodeId:NodeId,
             localAdapterServiceOption:Option[AdapterRequestHandler],
             breakdownTypes:Set[ResultOutputType],
             hubDao: HubDao
           ): HubComponents = {
    val broadcastDestinations: Set[NodeHandle]= NodeHandleSource.makeNodeHandles(hubConfig, keystoreTrustParam, nodeId, localAdapterServiceOption, breakdownTypes)

    val broadcaster: AdapterClientBroadcaster = AdapterClientBroadcaster(broadcastDestinations, hubDao)

    val broadcasterMultiplexerService: BroadcasterMultiplexerService = BroadcasterMultiplexerService(broadcaster, hubConfig.getConfigured("maxQueryWaitTime",DurationConfigParser(_)))

    val broadcasterMultiplexerResource:BroadcasterMultiplexerResource = BroadcasterMultiplexerResource(broadcasterMultiplexerService)

    new HubComponents(broadcastDestinations, broadcasterMultiplexerService,broadcasterMultiplexerResource)
  }
}