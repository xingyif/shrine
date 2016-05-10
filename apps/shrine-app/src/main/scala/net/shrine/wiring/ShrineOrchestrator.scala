package net.shrine.wiring

import javax.sql.DataSource

import com.typesafe.config.{Config, ConfigFactory}
import net.shrine.adapter.dao.squeryl.tables.{Tables => AdapterTables}
import net.shrine.adapter.dao.squeryl.{SquerylAdapterDao, SquerylI2b2AdminDao}
import net.shrine.adapter.dao.{AdapterDao, I2b2AdminDao}
import net.shrine.adapter.service.{AdapterRequestHandler, AdapterResource, AdapterService, I2b2AdminResource, I2b2AdminService}
import net.shrine.adapter.translators.{ExpressionTranslator, QueryDefinitionTranslator}
import net.shrine.adapter.{Adapter, AdapterMap, DeleteQueryAdapter, FlagQueryAdapter, ReadInstanceResultsAdapter, ReadPreviousQueriesAdapter, ReadQueryDefinitionAdapter, ReadQueryResultAdapter, ReadTranslatedQueryDefinitionAdapter, RenameQueryAdapter, RunQueryAdapter, UnFlagQueryAdapter}
import net.shrine.authentication.Authenticator
import net.shrine.authorization.QueryAuthorizationService
import net.shrine.broadcaster.dao.HubDao
import net.shrine.broadcaster.dao.squeryl.SquerylHubDao
import net.shrine.broadcaster.service.{BroadcasterMultiplexerResource, BroadcasterMultiplexerService}
import net.shrine.broadcaster.{AdapterClientBroadcaster, BroadcastAndAggregationService, NodeHandle, SigningBroadcastAndAggregationService}
import net.shrine.client.{EndpointConfig, JerseyHttpClient, OntClient, Poster, PosterOntClient}
import net.shrine.config.{ConfigExtensions, DurationConfigParser}
import net.shrine.config.mappings.{AdapterMappings, AdapterMappingsSource, ClasspathFormatDetectingAdapterMappingsSource}
import net.shrine.crypto.{DefaultSignerVerifier, KeyStoreCertCollection, KeyStoreDescriptorParser, TrustParam}
import net.shrine.dao.squeryl.{DataSourceSquerylInitializer, SquerylDbAdapterSelecter, SquerylInitializer}
import net.shrine.happy.{HappyShrineResource, HappyShrineService}
import net.shrine.log.Loggable
import net.shrine.ont.data.{OntClientOntologyMetadata, OntologyMetadata}
import net.shrine.protocol.{HiveCredentials, NodeId, RequestType, ResultOutputType, ResultOutputTypes}
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
  import NodeHandleSource.makeNodeHandles

  override def resources: Iterable[AnyRef] = {
    Seq(happyResource,statusJaxrs) ++ shrineResource ++ i2b2BroadcastResource ++ adapterResource ++ i2b2AdminResource ++ broadcasterMultiplexerResource
  }

  //todo another pass to put things only used in one place into that place's apply(Config)

  //Load config from file on the classpath called "shrine.conf"
  lazy val config: Config = ConfigFactory.load("shrine")

  val shrineConfig = config.getConfig("shrine")

  protected lazy val nodeId: NodeId = NodeId(shrineConfig.getString("humanReadableNodeName"))

  //TODO: Don't assume keystore lives on the filesystem, could come from classpath, etc
  lazy val keyStoreDescriptor = shrineConfig.getConfigured("keystore",KeyStoreDescriptorParser(_))
  lazy val shrineCertCollection: KeyStoreCertCollection = KeyStoreCertCollection.fromFile(keyStoreDescriptor)
  protected lazy val keystoreTrustParam: TrustParam = TrustParam.SomeKeyStore(shrineCertCollection)
  //todo see if you can remove this by pushing it closer to where it gets used
  lazy val signerVerifier: DefaultSignerVerifier = new DefaultSignerVerifier(shrineCertCollection)

  protected lazy val dataSource: DataSource = Jndi("java:comp/env/jdbc/shrineDB").get
  protected lazy val squerylAdapter: DatabaseAdapter = SquerylDbAdapterSelecter.determineAdapter(shrineConfig.getString("shrineDatabaseType"))
  protected lazy val squerylInitializer: SquerylInitializer = new DataSourceSquerylInitializer(dataSource, squerylAdapter)

  private lazy val pmEndpoint: EndpointConfig = shrineConfig.getConfigured("pmEndpoint", EndpointConfig(_))
  lazy val pmPoster: Poster = Poster(shrineCertCollection,pmEndpoint)

  private lazy val ontEndpoint: EndpointConfig = shrineConfig.getConfigured("ontEndpoint", EndpointConfig(_))
  protected lazy val ontPoster: Poster = Poster(shrineCertCollection,ontEndpoint)

  protected lazy val breakdownTypes: Set[ResultOutputType] = shrineConfig.getOptionConfigured("breakdownResultOutputTypes", ResultOutputTypes.fromConfig).getOrElse(Set.empty)

  protected lazy val hubDao: HubDao = new SquerylHubDao(squerylInitializer, new net.shrine.broadcaster.dao.squeryl.tables.Tables)

  lazy val crcHiveCredentials = shrineConfig.getConfigured("hiveCredentials", HiveCredentials(_, HiveCredentials.CRC))

  //todo here's the Adapter. Move to the adapter package.
  case class AdapterComponents(adapterService: AdapterService, i2b2AdminService: I2b2AdminService, adapterDao: AdapterDao, adapterMappings: AdapterMappings)

  object AdapterComponents {
    def apply(adapterConfig:Config):AdapterComponents = { //config is "shrine.adapter"
      val crcEndpoint: EndpointConfig = adapterConfig.getConfigured("crcEndpoint",EndpointConfig(_))

      val crcPoster: Poster = Poster(shrineCertCollection,crcEndpoint)

      val squerylAdapterTables: AdapterTables = new AdapterTables

      val adapterDao: AdapterDao = new SquerylAdapterDao(squerylInitializer, squerylAdapterTables)(breakdownTypes)

      //NB: Is i2b2HiveCredentials.projectId the right project id to use?
      val i2b2AdminDao: I2b2AdminDao = new SquerylI2b2AdminDao(crcHiveCredentials.projectId, squerylInitializer, squerylAdapterTables)

      val adapterMappingsSource: AdapterMappingsSource = ClasspathFormatDetectingAdapterMappingsSource(adapterConfig.getString("adapterMappingsFileName"))

      //NB: Fail fast
      val adapterMappings: AdapterMappings = adapterMappingsSource.load.get

      val expressionTranslator: ExpressionTranslator = ExpressionTranslator(adapterMappings)

      val queryDefinitionTranslator: QueryDefinitionTranslator = new QueryDefinitionTranslator(expressionTranslator)

      val doObfuscation = adapterConfig.getBoolean("setSizeObfuscation")
      val collectAdapterAudit = adapterConfig.getBoolean("audit.collectAdapterAudit") //todo figure out testing

      val runQueryAdapter = new RunQueryAdapter(
        crcPoster,
        adapterDao,
        crcHiveCredentials,
        queryDefinitionTranslator,
        adapterConfig.getInt("adapterLockoutAttemptsThreshold"),
        doObfuscation,
        adapterConfig.getOption("immediatelyRunIncomingQueries", _.getBoolean).getOrElse(true), //todo use reference.conf
        breakdownTypes,
        collectAdapterAudit
      )

      val readInstanceResultsAdapter: Adapter = new ReadInstanceResultsAdapter(
        crcPoster,
        crcHiveCredentials,
        adapterDao,
        doObfuscation,
        breakdownTypes,
        collectAdapterAudit
      )

      val readQueryResultAdapter: Adapter = new ReadQueryResultAdapter(
        crcPoster,
        crcHiveCredentials,
        adapterDao,
        doObfuscation,
        breakdownTypes,
        collectAdapterAudit
      )

      val readPreviousQueriesAdapter: Adapter = new ReadPreviousQueriesAdapter(adapterDao)

      val deleteQueryAdapter: Adapter = new DeleteQueryAdapter(adapterDao)

      val renameQueryAdapter: Adapter = new RenameQueryAdapter(adapterDao)

      val readQueryDefinitionAdapter: Adapter = new ReadQueryDefinitionAdapter(adapterDao)

      val readTranslatedQueryDefinitionAdapter: Adapter = new ReadTranslatedQueryDefinitionAdapter(nodeId, queryDefinitionTranslator)

      val flagQueryAdapter: Adapter = new FlagQueryAdapter(adapterDao)

      val unFlagQueryAdapter: Adapter = new UnFlagQueryAdapter(adapterDao)

      val adapterMap = AdapterMap(Map(
        RequestType.QueryDefinitionRequest -> runQueryAdapter,
        RequestType.GetRequestXml -> readQueryDefinitionAdapter,
        RequestType.UserRequest -> readPreviousQueriesAdapter,
        RequestType.InstanceRequest -> readInstanceResultsAdapter,
        RequestType.MasterDeleteRequest -> deleteQueryAdapter,
        RequestType.MasterRenameRequest -> renameQueryAdapter,
        RequestType.GetQueryResult -> readQueryResultAdapter,
        RequestType.ReadTranslatedQueryDefinitionRequest -> readTranslatedQueryDefinitionAdapter,
        RequestType.FlagQueryRequest -> flagQueryAdapter,
        RequestType.UnFlagQueryRequest -> unFlagQueryAdapter))

      AdapterComponents(
        adapterService = new AdapterService(
          nodeId = nodeId,
          signatureVerifier = signerVerifier,
          maxSignatureAge = adapterConfig.getConfigured("maxSignatureAge", DurationConfigParser(_)),
          adapterMap = adapterMap
        ),
        i2b2AdminService = new I2b2AdminService(
          dao = adapterDao,
          i2b2AdminDao = i2b2AdminDao,
          pmPoster = pmPoster,
          runQueryAdapter = runQueryAdapter
        ),
        adapterDao = adapterDao,
        adapterMappings = adapterMappings)
    }
  }

  val adapterComponents:Option[AdapterComponents] = shrineConfig.getOptionConfiguredIf("adapter", AdapterComponents(_))

  //todo maybe just break demeter too use these
  lazy val adapterService: Option[AdapterService] = adapterComponents.map(_.adapterService)
  lazy val i2b2AdminService: Option[I2b2AdminService] = adapterComponents.map(_.i2b2AdminService)
  lazy val adapterDao: Option[AdapterDao] = adapterComponents.map(_.adapterDao)
  lazy val adapterMappings: Option[AdapterMappings] = adapterComponents.map(_.adapterMappings)

  val shouldQuerySelf = "hub.shouldQuerySelf"
  lazy val localAdapterServiceOption: Option[AdapterRequestHandler] = if(shrineConfig.getOption(shouldQuerySelf,_.getBoolean).getOrElse(false)) { //todo give this a default value (of false) in the reference.conf for the Hub, and make it part of the Hub's apply(config)
    require(adapterService.isDefined, s"Self-querying requested because shrine.$shouldQuerySelf is true, but this node is not configured to have an adapter")
    adapterService
  }
    else None //todo eventually make this just another downstream node accessed via loopback

  //todo anything that uses hubConfig should be inside the big Hub component. broadcastDesitnations leak out because the QEP doesn't use loopback to talk to itself.
  //todo as an easy earlier step, make the hub first, then the QEP. Ask the hub for its destinations
  val hubConfig = shrineConfig.getConfig("hub")

  //todo use an empty Set instead of Option[Set]
  private lazy val broadcastDestinations: Option[Set[NodeHandle]] = {
    if(hubConfig.getBoolean("create")) {
      Some(makeNodeHandles(hubConfig, keystoreTrustParam, nodeId, localAdapterServiceOption, breakdownTypes))
    }
    else None
  }

  //todo a hub component. Gather them all up
  private lazy val broadcasterOption: Option[AdapterClientBroadcaster] = {
    if(hubConfig.getBoolean("create")) {
      require(broadcastDestinations.isDefined, "This node is configured to be a hub, but no downstream nodes are defined")
      Some(AdapterClientBroadcaster(broadcastDestinations.get, hubDao))
    }
    else None
  }

  //todo a hub component
  lazy val broadcasterMultiplexerService: Option[BroadcasterMultiplexerService] = broadcasterOption.map(BroadcasterMultiplexerService(_, hubConfig.getConfigured("maxQueryWaitTime",DurationConfigParser(_))))

  //todo anything that requires qepConfig should be inside QueryEntryPointComponents's apply
  protected lazy val qepConfig = shrineConfig.getConfig("queryEntryPoint")

  lazy val queryEntryPointComponents:Option[QueryEntryPointComponents] =
    if(qepConfig.getBoolean("create")) {

      val commonName: String = shrineCertCollection.myCommonName.getOrElse {
        val hostname = java.net.InetAddress.getLocalHost.getHostName
        warn(s"No common name available from ${shrineCertCollection.descriptor}. Using $hostname instead.")
        hostname
      }

      val broadcastService: BroadcastAndAggregationService = SigningBroadcastAndAggregationService(
        qepConfig,
        shrineCertCollection,
        breakdownTypes,
        broadcastDestinations,
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
  
  protected lazy val broadcasterMultiplexerResource: Option[BroadcasterMultiplexerResource] = broadcasterMultiplexerService.map(BroadcasterMultiplexerResource(_))

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
