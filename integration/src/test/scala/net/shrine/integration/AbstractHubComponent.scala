package net.shrine.integration

import net.shrine.adapter.client.RemoteAdapterClient
import net.shrine.adapter.service.JerseyTestComponent
import net.shrine.broadcaster.{AdapterClientBroadcaster, BroadcasterClient, InJvmBroadcasterClient, NodeHandle, PosterBroadcasterClient, SigningBroadcastAndAggregationService}
import net.shrine.client.ShrineClient
import net.shrine.crypto2.SigningCertStrategy
import net.shrine.protocol.{AuthenticationInfo, DefaultBreakdownResultOutputTypes, NodeId}

/**
 * @author clint
 * @since Jun 23, 2014
 */
abstract class AbstractHubComponent[H <: AnyRef](
    enclosingTest: AbstractHubAndSpokesTest, 
    override val basePath: String,
    override val port: Int) extends JerseyTestComponent[H] {
  
  lazy val broadcaster: InspectableDelegatingBroadcaster = {
    import enclosingTest.{posterFor, spokes}

    val destinations: Set[NodeHandle] = spokes.map { spoke =>
      val client = RemoteAdapterClient(NodeId.Unknown,posterFor(spoke), DefaultBreakdownResultOutputTypes.toSet)

      NodeHandle(spoke.nodeId, client)
    }

    InspectableDelegatingBroadcaster(AdapterClientBroadcaster(destinations, MockHubDao))
  }
  
  def clientFor(projectId: String, networkAuthn: AuthenticationInfo): ShrineClient
  
  protected def inJvmBroadcasterClient: BroadcasterClient = InJvmBroadcasterClient(broadcaster)
  
  protected def posterBroadcasterClient(hubComponent: JerseyTestComponent[_]): BroadcasterClient = PosterBroadcasterClient(enclosingTest.posterFor(hubComponent), DefaultBreakdownResultOutputTypes.toSet)
  
  protected def signingBroadcastService(broadcasterClient: BroadcasterClient): SigningBroadcastAndAggregationService = SigningBroadcastAndAggregationService(broadcasterClient, enclosingTest.signerVerifier, SigningCertStrategy.Attach) 
}