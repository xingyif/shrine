package net.shrine.broadcaster

import com.typesafe.config.Config
import net.shrine.broadcaster.dao.HubDao
import net.shrine.client.{EndpointConfig, Poster}
import net.shrine.config.ConfigExtensions
import net.shrine.crypto2.{BouncyKeyStoreCollection, Signer, SignerVerifierAdapter, SigningCertStrategy}
import net.shrine.protocol.ResultOutputType

/**
 * @author clint
 * @since Feb 28, 2014
 */
final case class SigningBroadcastAndAggregationService(broadcasterClient: BroadcasterClient,
                                                       signer: Signer,
                                                       signingCertStrategy: SigningCertStrategy)
  extends AbstractBroadcastAndAggregationService(broadcasterClient, signer.sign(_, signingCertStrategy))
{
  override def attachSigningCert: Boolean = signingCertStrategy == SigningCertStrategy.Attach
}

object SigningBroadcastAndAggregationService {

  def apply(qepConfig:Config,
            shrineCertCollection: BouncyKeyStoreCollection,
            breakdownTypes: Set[ResultOutputType], //todo I'm surprised you need this to support a remote hub. Figure out why. Remove if possible
            broadcastDestinations: Option[Set[NodeHandle]], //todo remove when you use loopback for a local hub
            hubDao: HubDao //todo remove when you use loopback for a local hub
           ):SigningBroadcastAndAggregationService = {

    val signerVerifier: Signer = SignerVerifierAdapter(shrineCertCollection)

    val broadcasterClient: BroadcasterClient = {
      //todo don't bother with a distinction between local and remote QEPs. Just use loopback.
      val remoteHubEndpoint = qepConfig.getOptionConfigured("broadcasterServiceEndpoint", EndpointConfig(_))
      remoteHubEndpoint.fold{
        require(broadcastDestinations.isDefined, s"The QEP's config implied a local hub (no broadcasterServiceEndpoint), but either no downstream nodes were configured, the hub was not configured, or the hub's configuration specified not to create it.")

        val broadcaster: AdapterClientBroadcaster = AdapterClientBroadcaster(broadcastDestinations.get, hubDao)

        val broadcastClient:BroadcasterClient = InJvmBroadcasterClient(broadcaster)
        broadcastClient
      }{ hubEndpointConfig =>
        PosterBroadcasterClient(Poster(shrineCertCollection,hubEndpointConfig), breakdownTypes)
      }
    }

    //todo ditch the option and use reference.conf
    val attachSigningCerts: Boolean = qepConfig.getOption("attachSigningCert", _.getBoolean).getOrElse(false)
    val signingCertStrategy:SigningCertStrategy = if(attachSigningCerts) SigningCertStrategy.Attach else SigningCertStrategy.DontAttach

    new SigningBroadcastAndAggregationService(broadcasterClient, signerVerifier, signingCertStrategy)
  }

}