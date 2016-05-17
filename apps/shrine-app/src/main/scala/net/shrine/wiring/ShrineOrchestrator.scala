package net.shrine.wiring

import javax.sql.DataSource

import com.typesafe.config.{Config, ConfigFactory}
import net.shrine.adapter.AdapterComponents
import net.shrine.adapter.dao.AdapterDao
import net.shrine.adapter.service.{AdapterRequestHandler, AdapterResource, AdapterService, I2b2AdminResource, I2b2AdminService}
import net.shrine.authentication.Authenticator
import net.shrine.authorization.QueryAuthorizationService
import net.shrine.broadcaster.dao.HubDao
import net.shrine.broadcaster.dao.squeryl.SquerylHubDao
import net.shrine.broadcaster.service.HubComponents
import net.shrine.broadcaster.{BroadcastAndAggregationService, SigningBroadcastAndAggregationService}
import net.shrine.client.{EndpointConfig, JerseyHttpClient, OntClient, Poster, PosterOntClient}
import net.shrine.config.ConfigExtensions
import net.shrine.config.mappings.AdapterMappings
import net.shrine.crypto.{DefaultSignerVerifier, KeyStoreCertCollection, KeyStoreDescriptorParser, TrustParam}
import net.shrine.dao.squeryl.{DataSourceSquerylInitializer, SquerylDbAdapterSelecter, SquerylInitializer}
import net.shrine.happy.{HappyShrineResource, HappyShrineService}
import net.shrine.log.Loggable
import net.shrine.ont.data.{OntClientOntologyMetadata, OntologyMetadata}
import net.shrine.protocol.{HiveCredentials, NodeId, ResultOutputType, ResultOutputTypes}
import net.shrine.qep.dao.AuditDao
import net.shrine.qep.dao.squeryl.SquerylAuditDao
import net.shrine.qep.dao.squeryl.tables.{Tables => HubTables}
import net.shrine.qep.{I2b2BroadcastResource, I2b2QepService, QepService, ShrineResource}
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
    Seq(happyResource,statusJaxrs) ++
      shrineResource ++
      i2b2BroadcastResource ++
      adapterResource ++
      i2b2AdminResource ++
      hubComponents.map(_.broadcasterMultiplexerResource)
  }

  //todo another pass to put things only used in one place into that place's apply(Config)

  //Load config from file on the classpath called "shrine.conf"
  lazy val config: Config = ConfigFactory.load("shrine")

  val shrineConfig = config.getConfig("shrine")

  protected lazy val nodeId: NodeId = NodeId(shrineConfig.getString("humanReadableNodeName"))

  //TODO: Don't assume keystore lives on the filesystem, could come from classpath, etc
  lazy val keyStoreDescriptor = shrineConfig.getConfigured("keystore",KeyStoreDescriptorParser(_))
  lazy val certCollection: KeyStoreCertCollection = KeyStoreCertCollection.fromFile(keyStoreDescriptor)
  protected lazy val keystoreTrustParam: TrustParam = TrustParam.SomeKeyStore(certCollection)
  //todo used by the adapterServide and happyShrineService, but not by the QEP. maybe each can have its own signerVerivier
  lazy val signerVerifier: DefaultSignerVerifier = new DefaultSignerVerifier(certCollection)

  protected lazy val dataSource: DataSource = Jndi("java:comp/env/jdbc/shrineDB").get
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

  val hubConfig = shrineConfig.getConfig("hub")

  lazy val hubComponents: Option[HubComponents] = shrineConfig.getOptionConfiguredIf("hub",HubComponents(_,
    keystoreTrustParam,
    nodeId,
    localAdapterServiceOption,
    breakdownTypes,
    hubDao
  ))

  //todo anything that requires qepConfig should be inside QueryEntryPointComponents's apply
  protected lazy val qepConfig = shrineConfig.getConfig("queryEntryPoint")

  lazy val queryEntryPointComponents:Option[QueryEntryPointComponents] =
    if(qepConfig.getBoolean("create")) {

      val commonName: String = certCollection.myCommonName.getOrElse {
        val hostname = java.net.InetAddress.getLocalHost.getHostName
        warn(s"No common name available from ${certCollection.descriptor}. Using $hostname instead.")
        hostname
      }

      val broadcastService: BroadcastAndAggregationService = SigningBroadcastAndAggregationService(
        qepConfig,
        certCollection,
        breakdownTypes,
        hubComponents.map(_.broadcastDestinations),
        hubDao
      )

      val auditDao: AuditDao = new SquerylAuditDao(squerylInitializer, new HubTables)
      val authenticator: Authenticator = AuthStrategy.determineAuthenticator(qepConfig, pmPoster)
      val authorizationService: QueryAuthorizationService = AuthStrategy.determineQueryAuthorizationService(qepConfig,authenticator)

      debug(s"authorizationService set to $authorizationService")

      Some(QueryEntryPointComponents(
          qepConfig,
          commonName,
          auditDao,
          authenticator,
          authorizationService,
          broadcastService
        ))
    }
    else None

  protected lazy val pmUrlString: String = pmEndpoint.url.toString

  private lazy val ontEndpoint: EndpointConfig = shrineConfig.getConfigured("ontEndpoint", EndpointConfig(_))
  protected lazy val ontPoster: Poster = Poster(certCollection,ontEndpoint)

  //todo only used by happy outside of here
  lazy val ontologyMetadata: OntologyMetadata = {
    import scala.concurrent.duration._

    //TODO: XXX: Un-hard-code max wait time param
    val ontClient: OntClient = new PosterOntClient(shrineConfig.getConfigured("hiveCredentials", HiveCredentials(_, HiveCredentials.ONT)), 1.minute, ontPoster)

    new OntClientOntologyMetadata(ontClient)
  }

  protected lazy val happyResource: HappyShrineResource = new HappyShrineResource(HappyShrineService)

  protected lazy val statusJaxrs: StatusJaxrs = StatusJaxrs(config)

  protected lazy val shrineResource: Option[ShrineResource] = queryEntryPointComponents.map(x => ShrineResource(x.shrineService))

  protected lazy val i2b2BroadcastResource: Option[I2b2BroadcastResource] = queryEntryPointComponents.map(x => new I2b2BroadcastResource(x.i2b2Service,breakdownTypes))

  protected lazy val adapterResource: Option[AdapterResource] = adapterService.map(AdapterResource(_))

  protected lazy val i2b2AdminResource: Option[I2b2AdminResource] = i2b2AdminService.map(I2b2AdminResource(_, breakdownTypes))
  
  //todo here's the QEP. Move to the QEP package.
  case class QueryEntryPointComponents(shrineService: QepService,
                                       i2b2Service: I2b2QepService,
                                       auditDao: AuditDao) //todo auditDao is only used by the happy service to grab the most recent entries

  object QueryEntryPointComponents {
    def apply(
      qepConfig:Config,
      commonName: String,
      auditDao: AuditDao,
      authenticator: Authenticator,
      authorizationService: QueryAuthorizationService,
      broadcastService: BroadcastAndAggregationService
    ):QueryEntryPointComponents = {
      QueryEntryPointComponents(
        QepService(
          qepConfig,
          commonName,
          auditDao,
          authenticator,
          authorizationService,
          broadcastService,
          breakdownTypes
        ),
        I2b2QepService(
          qepConfig,
          commonName,
          auditDao,
          authenticator,
          authorizationService,
          broadcastService,
          breakdownTypes
        ),
        auditDao)
    }
  }

  def poster(keystoreCertCollection: KeyStoreCertCollection)(endpoint: EndpointConfig): Poster = {
    val httpClient = JerseyHttpClient(keystoreCertCollection, endpoint)

    Poster(endpoint.url.toString, httpClient)
  }
}