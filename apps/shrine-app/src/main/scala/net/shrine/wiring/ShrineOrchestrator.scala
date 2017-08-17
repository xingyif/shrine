package net.shrine.wiring

import javax.sql.DataSource

import com.typesafe.config.Config
import net.shrine.adapter.AdapterComponents
import net.shrine.adapter.dao.AdapterDao
import net.shrine.adapter.service.{AdapterRequestHandler, AdapterResource, AdapterService, I2b2AdminResource, I2b2AdminService}
import net.shrine.broadcaster.dao.HubDao
import net.shrine.broadcaster.dao.squeryl.SquerylHubDao
import net.shrine.broadcaster.service.HubComponents
import net.shrine.client.{EndpointConfig, JerseyHttpClient, OntClient, Poster, PosterOntClient}
import net.shrine.config.ConfigExtensions
import net.shrine.config.mappings.AdapterMappings
import net.shrine.crypto.{BouncyKeyStoreCollection, KeyStoreDescriptorParser, SignerVerifierAdapter, TrustParam}
import net.shrine.dao.squeryl.{DataSourceSquerylInitializer, SquerylDbAdapterSelecter, SquerylInitializer}
import net.shrine.log.Loggable
import net.shrine.ont.data.OntClientOntologyMetadata
import net.shrine.protocol.{HiveCredentials, NodeId, ResultOutputType, ResultOutputTypes}
import net.shrine.qep.{I2b2BroadcastResource, QueryEntryPointComponents, ShrineResource}
import net.shrine.slick.TestableDataSourceCreator
import net.shrine.source.ConfigSource
import net.shrine.status.StatusJaxrs
import org.squeryl.internals.DatabaseAdapter

/**
 * @author clint
 * @since Jan 14, 2014
 *
 * Application wiring for Shrine.
 */
object ShrineOrchestrator extends ShrineJaxrsResources with Loggable {

  override def resources: Iterable[AnyRef] = {
    Seq(statusJaxrs) ++
      shrineResource ++
      i2b2BroadcastResource ++
      adapterResource ++
      i2b2AdminResource ++
      hubComponents.map(_.broadcasterMultiplexerResource)
  }

  //todo another pass to put things only used in one place into that place's apply(Config)

  //Load config from file on the classpath called "shrine.conf"
  lazy val config: Config = ConfigSource.config

  //todo for SHRINE-2120 , can access the QEP's node name from config!
  val shrineConfig = config.getConfig("shrine")

  protected lazy val nodeId: NodeId = NodeId(shrineConfig.getString("humanReadableNodeName"))

  //TODO: Don't assume keystore lives on the filesystem, could come from classpath, etc
  lazy val keyStoreDescriptor = KeyStoreDescriptorParser(shrineConfig.getConfig("keystore"), shrineConfig.getConfigOrEmpty("hub"), shrineConfig.getConfigOrEmpty("queryEntryPoint"))
  lazy val certCollection: BouncyKeyStoreCollection = BouncyKeyStoreCollection.fromFileRecoverWithClassPath(keyStoreDescriptor)
  protected lazy val keystoreTrustParam: TrustParam = TrustParam.BouncyKeyStore(certCollection)
  //todo used by the adapterService and happyShrineService, but not by the QEP. maybe each can have its own signerVerifier
  lazy val signerVerifier = SignerVerifierAdapter(certCollection)
  protected lazy val dataSource: DataSource = TestableDataSourceCreator.dataSource(shrineConfig.getConfig("squerylDataSource.database"))
  protected lazy val squerylAdapter: DatabaseAdapter = SquerylDbAdapterSelecter.determineAdapter(shrineConfig.getString("shrineDatabaseType"))
  protected lazy val squerylInitializer: SquerylInitializer = new DataSourceSquerylInitializer(dataSource, squerylAdapter)

  //todo it'd be better for the adapter and qep to each have its own connection to the pm cell.
  private lazy val pmEndpoint: EndpointConfig = shrineConfig.getConfigured("pmEndpoint", EndpointConfig(_))
  lazy val pmPoster: Poster = Poster(certCollection,pmEndpoint)

  protected lazy val breakdownTypes: Set[ResultOutputType] = shrineConfig.getOptionConfigured("breakdownResultOutputTypes", ResultOutputTypes.fromConfig).getOrElse(Set.empty)

  //todo why does the qep need a HubDao ?
  protected lazy val hubDao: HubDao = new SquerylHubDao(squerylInitializer, new net.shrine.broadcaster.dao.squeryl.tables.Tables)

//todo really should be part of the adapter config, but is out in shrine's part of the name space
  lazy val crcHiveCredentials: HiveCredentials = shrineConfig.getConfigured("hiveCredentials", HiveCredentials(_, HiveCredentials.CRC))

  val adapterComponents:Option[AdapterComponents] = shrineConfig.getOptionConfiguredIf("adapter", AdapterComponents(
    _,
    certCollection,
    squerylInitializer,
    breakdownTypes,
    crcHiveCredentials,
    signerVerifier,
    pmPoster,
    nodeId
  ))
  if (adapterComponents.isEmpty)
    warn("Adapter Components is improperly configured, please check the adapter section in shrine.conf")

  //todo maybe just break demeter too use this
  lazy val adapterService: Option[AdapterService] = adapterComponents.map(_.adapterService)
  //todo maybe just break demeter too use this
  lazy val i2b2AdminService: Option[I2b2AdminService] = adapterComponents.map(_.i2b2AdminService)
  //todo this is only used by happy
  lazy val adapterDao: Option[AdapterDao] = adapterComponents.map(_.adapterDao)
  //todo this is only used by happy
  lazy val adapterMappings: Option[AdapterMappings] = adapterComponents.map(_.adapterMappings)

  val shouldQuerySelf = "hub.shouldQuerySelf"
  lazy val localAdapterServiceOption: Option[AdapterRequestHandler] = if(shrineConfig.getOption(shouldQuerySelf,_.getBoolean).getOrElse(false)) { //todo give this a default value (of false) in the reference.conf for the Hub, and make it part of the Hub's apply(config)
    require(adapterService.isDefined, s"Self-querying requested because shrine.$shouldQuerySelf is true, but this node is not configured to have an adapter")
    adapterService
  }
    else None //todo eventually make this just another downstream node accessed via loopback

  lazy val hubComponents: Option[HubComponents] = shrineConfig.getOptionConfiguredIf("hub",HubComponents(_,
    keystoreTrustParam,
    nodeId,
    localAdapterServiceOption,
    breakdownTypes,
    hubDao
  ))

  //todo anything that requires qepConfig should be inside QueryEntryPointComponents's apply
  protected lazy val qepConfig = shrineConfig.getConfig("queryEntryPoint")

  lazy val queryEntryPointComponents:Option[QueryEntryPointComponents] = shrineConfig.getOptionConfiguredIf("queryEntryPoint", QueryEntryPointComponents(_,
    certCollection,
    breakdownTypes,
    hubComponents.map(_.broadcastDestinations),
    hubDao, //todo the QEP should not need the hub dao
    squerylInitializer, //todo could really have its own
    pmPoster //todo could really have its own
  ))

  protected lazy val pmUrlString: String = pmEndpoint.url.toString

  private[shrine] lazy val ontEndpoint: EndpointConfig = shrineConfig.getConfigured("ontEndpoint", EndpointConfig(_))
  protected lazy val ontPoster: Poster = Poster(certCollection,ontEndpoint)

  //todo only used by happy outside of here
  lazy val ontologyMetadata: OntClientOntologyMetadata = {
    import scala.concurrent.duration._

    //TODO: XXX: Un-hard-code max wait time param
    val ontClient: OntClient = new PosterOntClient(shrineConfig.getConfigured("hiveCredentials", HiveCredentials(_, HiveCredentials.ONT)), 1.minute, ontPoster)

    new OntClientOntologyMetadata(ontClient)
  }

  protected lazy val statusJaxrs: StatusJaxrs = StatusJaxrs(config)

  protected lazy val shrineResource: Option[ShrineResource] = queryEntryPointComponents.map(x => ShrineResource(x.shrineService))

  protected lazy val i2b2BroadcastResource: Option[I2b2BroadcastResource] = queryEntryPointComponents.map(x => new I2b2BroadcastResource(x.i2b2Service,breakdownTypes))

  protected lazy val adapterResource: Option[AdapterResource] = adapterService.map(AdapterResource(_))

  protected lazy val i2b2AdminResource: Option[I2b2AdminResource] = i2b2AdminService.map(I2b2AdminResource(_, breakdownTypes))



  def poster(keystoreCertCollection: BouncyKeyStoreCollection)(endpoint: EndpointConfig): Poster = {
    val httpClient = JerseyHttpClient(keystoreCertCollection, endpoint)

    Poster(endpoint.url.toString, httpClient)
  }
}

