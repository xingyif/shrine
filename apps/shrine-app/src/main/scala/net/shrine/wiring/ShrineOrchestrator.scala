package net.shrine.wiring

import javax.sql.DataSource


import com.typesafe.config.{Config, ConfigFactory}
import net.shrine.adapter.dao.squeryl.tables.{Tables => AdapterTables}
import net.shrine.adapter.dao.squeryl.{SquerylAdapterDao, SquerylI2b2AdminDao}
import net.shrine.adapter.dao.{AdapterDao, I2b2AdminDao}
import net.shrine.adapter.service.{AdapterConfig, AdapterRequestHandler, AdapterResource, AdapterService, I2b2AdminResource, I2b2AdminService}
import net.shrine.adapter.translators.{ExpressionTranslator, QueryDefinitionTranslator}
import net.shrine.adapter.{Adapter, AdapterMap, DeleteQueryAdapter, FlagQueryAdapter, ReadInstanceResultsAdapter, ReadPreviousQueriesAdapter, ReadQueryDefinitionAdapter, ReadQueryResultAdapter, ReadTranslatedQueryDefinitionAdapter, RenameQueryAdapter, RunQueryAdapter, UnFlagQueryAdapter}
import net.shrine.authentication.Authenticator
import net.shrine.authorization.QueryAuthorizationService
import net.shrine.broadcaster.dao.HubDao
import net.shrine.broadcaster.dao.squeryl.SquerylHubDao
import net.shrine.broadcaster.service.{BroadcasterMultiplexerResource, BroadcasterMultiplexerService}
import net.shrine.broadcaster.{AdapterClientBroadcaster, BroadcastAndAggregationService, BroadcasterClient, InJvmBroadcasterClient, NodeHandle, PosterBroadcasterClient, SigningBroadcastAndAggregationService}
import net.shrine.client.{EndpointConfig, HttpClient, JerseyHttpClient, OntClient, Poster, PosterOntClient}
import net.shrine.config.ConfigExtensions
import net.shrine.config.mappings.{AdapterMappings, AdapterMappingsSource, ClasspathFormatDetectingAdapterMappingsSource}
import net.shrine.crypto.{DefaultSignerVerifier, KeyStoreCertCollection, KeyStoreDescriptorParser, TrustParam}
import net.shrine.dao.squeryl.{DataSourceSquerylInitializer, SquerylDbAdapterSelecter, SquerylInitializer}
import net.shrine.happy.{HappyShrineResource, HappyShrineService}
import net.shrine.log.Loggable
import net.shrine.ont.data.{OntClientOntologyMetadata, OntologyMetadata}
import net.shrine.protocol.{HiveCredentials, ResultOutputTypes, NodeId, RequestType, ResultOutputType}
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
 * Application wiring for Shrine, in the base, non-HMS case.  All vals are protecetd, so they may be accessed,
 * in subclasses without ballooning this class's public API, and lazy, to work around init-order surprises when
 * overriding vals declared inline.  See
 *
 * https://stackoverflow.com/questions/15762650/scala-override-val-in-class-inheritance
 *
 * among other links mentioning val overrides, early initializers, etc. -Clint
 */
object ShrineOrchestrator extends ShrineJaxrsResources with Loggable {
  import NodeHandleSource.makeNodeHandles

  override def resources: Iterable[AnyRef] = {
    Seq(happyResource,statusJaxrs) ++ shrineResource ++ i2b2BroadcastResource ++ adapterResource ++ i2b2AdminResource ++ broadcasterMultiplexerResource
  }
  
  //Load config from file on the classpath called "shrine.conf"
  lazy val config: Config = ConfigFactory.load("shrine")

  protected lazy val shrineConfigurationBall: ShrineConfig = ShrineConfig(config)

  val shrineConfig = config.getConfig("shrine")

  protected lazy val nodeId: NodeId = NodeId(shrineConfig.getString("humanReadableNodeName"))

  //TODO: Don't assume keystore lives on the filesystem, could come from classpath, etc
  protected lazy val keyStoreDescriptor = shrineConfig.getConfigured("keystore",KeyStoreDescriptorParser(_))
  protected lazy val shrineCertCollection: KeyStoreCertCollection = KeyStoreCertCollection.fromFile(keyStoreDescriptor)
  protected lazy val keystoreTrustParam: TrustParam = TrustParam.SomeKeyStore(shrineCertCollection)
  protected lazy val signerVerifier: DefaultSignerVerifier = new DefaultSignerVerifier(shrineCertCollection)

  protected lazy val dataSource: DataSource = Jndi("java:comp/env/jdbc/shrineDB").get
  protected lazy val squerylAdapter: DatabaseAdapter = SquerylDbAdapterSelecter.determineAdapter(shrineConfig.getString("shrineDatabaseType"))
  protected lazy val squerylInitializer: SquerylInitializer = new DataSourceSquerylInitializer(dataSource, squerylAdapter)

  private def makePoster = poster(shrineCertCollection) _

  private lazy val pmEndpoint: EndpointConfig = shrineConfig.getConfigured("pmEndpoint", EndpointConfig(_))
  protected lazy val pmPoster: Poster = makePoster(pmEndpoint)

  private lazy val ontEndpoint: EndpointConfig = shrineConfig.getConfigured("ontEndpoint", EndpointConfig(_))
  protected lazy val ontPoster: Poster = makePoster(ontEndpoint)

  protected lazy val breakdownTypes: Set[ResultOutputType] = shrineConfig.getOptionConfigured("breakdownResultOutputTypes", ResultOutputTypes.fromConfig).getOrElse(Set.empty)

  protected lazy val hubDao: HubDao = new SquerylHubDao(squerylInitializer, new net.shrine.broadcaster.dao.squeryl.tables.Tables)

  lazy val crcHiveCredentials = shrineConfig.getConfigured("hiveCredentials", HiveCredentials(_, HiveCredentials.CRC))

  //todo move as much of this block as possible to the adapter project, and get rid of this multi-assignment of one thing
  protected lazy val (
    adapterService: Option[AdapterService],
    i2b2AdminService: Option[I2b2AdminService],
    adapterDao: Option[AdapterDao],
    adapterMappings: Option[AdapterMappings]
  ) = adapterComponentsToTuple(shrineConfig.getOptionConfiguredIf("adapter", AdapterConfig(_)).map { adapterConfig => //todo unwind adapterConfig and just have an adapter

    val crcEndpoint: EndpointConfig = adapterConfig.crcEndpoint

    val crcPoster: Poster = makePoster(crcEndpoint)

    val squerylAdapterTables: AdapterTables = new AdapterTables

    val adapterDao: AdapterDao = new SquerylAdapterDao(squerylInitializer, squerylAdapterTables)(breakdownTypes)

    //NB: Is i2b2HiveCredentials.projectId the right project id to use?
    val i2b2AdminDao: I2b2AdminDao = new SquerylI2b2AdminDao(crcHiveCredentials.projectId, squerylInitializer, squerylAdapterTables)

    val adapterMappingsSource: AdapterMappingsSource = ClasspathFormatDetectingAdapterMappingsSource(adapterConfig.adapterMappingsFileName)

    //NB: Fail fast
    val adapterMappings: AdapterMappings = adapterMappingsSource.load.get

    val expressionTranslator: ExpressionTranslator = ExpressionTranslator(adapterMappings)

    val queryDefinitionTranslator: QueryDefinitionTranslator = new QueryDefinitionTranslator(expressionTranslator)

    val doObfuscation = adapterConfig.setSizeObfuscation

    val runQueryAdapter = new RunQueryAdapter(
      crcPoster,
      adapterDao,
      crcHiveCredentials,
      queryDefinitionTranslator,
      adapterConfig.adapterLockoutAttemptsThreshold,
      doObfuscation,
      adapterConfig.immediatelyRunIncomingQueries,
      breakdownTypes,
      collectAdapterAudit = adapterConfig.collectAdapterAudit
    )

    val readInstanceResultsAdapter: Adapter = new ReadInstanceResultsAdapter(
      crcPoster,
      crcHiveCredentials,
      adapterDao,
      doObfuscation,
      breakdownTypes,
      collectAdapterAudit = adapterConfig.collectAdapterAudit
    )

    val readQueryResultAdapter: Adapter = new ReadQueryResultAdapter(
      crcPoster,
      crcHiveCredentials,
      adapterDao,
      doObfuscation,
      breakdownTypes,
      collectAdapterAudit = adapterConfig.collectAdapterAudit
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
        new AdapterService(nodeId, signerVerifier, adapterConfig.maxSignatureAge, adapterMap),
        new I2b2AdminService(adapterDao, i2b2AdminDao, pmPoster, runQueryAdapter),
        adapterDao,
        adapterMappings)
  })

  val shouldQuerySelf = "hub.shouldQuerySelf"
  lazy val localAdapterServiceOption: Option[AdapterRequestHandler] = if(shrineConfig.getOption(shouldQuerySelf,_.getBoolean).getOrElse(false)) { //todo give this a default value (of false) in the reference.conf for the Hub, and make it part of the Hub's apply(config)
    require(adapterService.isDefined, s"Self-querying requested because shrine.$shouldQuerySelf is true, but this node is not configured to have an adapter")
    adapterService
  }
    else None //todo eventually make this just another downstream node accessed via loopback

  private lazy val broadcastDestinations: Option[Set[NodeHandle]] = shrineConfigurationBall.hubConfig.map(hubConfig => makeNodeHandles(keystoreTrustParam, hubConfig.maxQueryWaitTime, hubConfig.downstreamNodes, nodeId, localAdapterServiceOption, breakdownTypes))

  protected lazy val (shrineService, i2b2Service, auditDao) = queryEntryPointComponentsToTuple(shrineConfigurationBall.queryEntryPointConfig.map { queryEntryPointConfig =>

      val broadcasterClient: BroadcasterClient = {
        if(queryEntryPointConfig.broadcasterIsLocal) {
          //If broadcaster is local, we need a hub config
          //TODO: Enforce this when unmarshalling configs
          require(broadcastDestinations.isDefined, s"The QEP's config implied a local hub (no broadcasterServiceEndpoint), but either no downstream nodes were configured, the hub was not configured, or the hub's configuration specified not to create it.")

          val broadcaster: AdapterClientBroadcaster = AdapterClientBroadcaster(broadcastDestinations.get, hubDao)

          InJvmBroadcasterClient(broadcaster)
        } else {
          //if broadcaster is remote, we need an endpoint
          //TODO: Enforce this when unmarshalling configs
          require(queryEntryPointConfig.broadcasterServiceEndpoint.isDefined, "Non-local broadcaster requested, but no URL for the remote broadcaster is specified")

          PosterBroadcasterClient(makePoster(queryEntryPointConfig.broadcasterServiceEndpoint.get), breakdownTypes)
        }
      }

      val commonName:String = shrineCertCollection.myCommonName.getOrElse{
        val hostname = java.net.InetAddress.getLocalHost.getHostName
        warn(s"No common name available from ${shrineCertCollection.descriptor}. Using $hostname instead.")
        hostname
      }

      val broadcastService: BroadcastAndAggregationService = SigningBroadcastAndAggregationService(broadcasterClient, signerVerifier, queryEntryPointConfig.signingCertStrategy)

      val auditDao: AuditDao = new SquerylAuditDao(squerylInitializer, new HubTables)

      val authenticationType = queryEntryPointConfig.authenticationType

      val authorizationType = queryEntryPointConfig.authorizationType

      val authenticator: Authenticator = AuthStrategy.determineAuthenticator(authenticationType, pmPoster)

      val authorizationService: QueryAuthorizationService = AuthStrategy.determineQueryAuthorizationService(authorizationType, shrineConfigurationBall, authenticator)

      debug(s"authorizationService set to $authorizationService")

      QueryEntryPointComponents(
        QepService(
          commonName,
          auditDao,
          authenticator,
          authorizationService,
          queryEntryPointConfig.includeAggregateResults,
          broadcastService,
          queryEntryPointConfig.maxQueryWaitTime,
          breakdownTypes,
          queryEntryPointConfig.collectQepAudit
        ),
        I2b2QepService(
          commonName,
          auditDao,
          authenticator,
          authorizationService,
          queryEntryPointConfig.includeAggregateResults,
          broadcastService,
          queryEntryPointConfig.maxQueryWaitTime,
          breakdownTypes,
          queryEntryPointConfig.collectQepAudit
        ),
        auditDao)
    })

  private lazy val broadcasterOption = unpackHubComponents {
    for {
      hubConfig <- shrineConfigurationBall.hubConfig
    } yield {
      require(broadcastDestinations.isDefined, "This node is configured to be a hub, but no downstream nodes are defined")

      HubComponents(AdapterClientBroadcaster(broadcastDestinations.get, hubDao))
    }
  }

  protected lazy val broadcasterMultiplexerService = {
    for {
      broadcaster <- broadcasterOption
      hubConfig <- shrineConfigurationBall.hubConfig
    } yield {
      BroadcasterMultiplexerService(broadcaster, hubConfig.maxQueryWaitTime)
    }
  }

  protected lazy val pmUrlString: String = pmEndpoint.url.toString

  protected lazy val ontologyMetadata: OntologyMetadata = {
    import scala.concurrent.duration._

    //TODO: XXX: Un-hard-code max wait time param
    val ontClient: OntClient = new PosterOntClient(shrineConfig.getConfigured("hiveCredentials", HiveCredentials(_, HiveCredentials.ONT)), 1.minute, ontPoster)

    new OntClientOntologyMetadata(ontClient)
  }

  //TODO: Don't assume we're an adapter with an AdapterMappings (don't call .get)
  protected lazy val happyService: HappyShrineService = {
    new HappyShrineService(
      config = config,
      keystoreDescriptor = keyStoreDescriptor,
      shrineConfigObject = shrineConfigurationBall,
      certCollection = shrineCertCollection,
      signer = signerVerifier,
      pmPoster = pmPoster,
      ontologyMetadata = ontologyMetadata,
      adapterMappings = adapterMappings,
      auditDaoOption = auditDao,
      adapterDaoOption = adapterDao,
      broadcasterOption = broadcasterOption,
      adapterOption = adapterService
    )
  }

  protected lazy val happyResource: HappyShrineResource = new HappyShrineResource(happyService)

  protected lazy val statusJaxrs: StatusJaxrs = StatusJaxrs(config)

  protected lazy val shrineResource: Option[ShrineResource] = shrineService.map(ShrineResource(_))

  protected lazy val i2b2BroadcastResource: Option[I2b2BroadcastResource] = i2b2Service.map(new I2b2BroadcastResource(_, breakdownTypes))

  protected lazy val adapterResource: Option[AdapterResource] = adapterService.map(AdapterResource(_))

  protected lazy val i2b2AdminResource: Option[I2b2AdminResource] = i2b2AdminService.map(I2b2AdminResource(_, breakdownTypes))
  
  protected lazy val broadcasterMultiplexerResource: Option[BroadcasterMultiplexerResource] = broadcasterMultiplexerService.map(BroadcasterMultiplexerResource(_))

  def makeHttpClient(keystoreCertCollection: KeyStoreCertCollection, endpoint: EndpointConfig): HttpClient = {
    import TrustParam.{AcceptAllCerts, SomeKeyStore}

    val trustParam = if (endpoint.acceptAllCerts) AcceptAllCerts else SomeKeyStore(keystoreCertCollection)

    JerseyHttpClient(trustParam, endpoint.timeout)
  }

  private final case class AdapterComponents(adapterService: AdapterService, i2b2AdminService: I2b2AdminService, adapterDao: AdapterDao, adapterMappings: AdapterMappings)

  private final case class QueryEntryPointComponents(shrineService: QepService, i2b2Service: I2b2QepService, auditDao: AuditDao)

  private final case class HubComponents(broadcaster: AdapterClientBroadcaster)

  //TODO: TEST
  //todo get rid of this
  private def adapterComponentsToTuple(option: Option[AdapterComponents]): (Option[AdapterService], Option[I2b2AdminService], Option[AdapterDao], Option[AdapterMappings]) = option match {
    case None => (None, None, None, None)
    case Some(AdapterComponents(a, b, c, d)) => (Option(a), Option(b), Option(c), Option(d))
  }
  
  //TODO: TEST
  //todo get rid of this
  private def queryEntryPointComponentsToTuple(option: Option[QueryEntryPointComponents]): (Option[QepService], Option[I2b2QepService], Option[AuditDao]) = option match {
    case None => (None, None, None)
    case Some(QueryEntryPointComponents(a, b, c)) => (Option(a), Option(b), Option(c))
  }
  
  //TODO: TEST
  //todo get rid of this
  private def unpackHubComponents(option: Option[HubComponents]): Option[AdapterClientBroadcaster] = option.map(_.broadcaster)

  def poster(keystoreCertCollection: KeyStoreCertCollection)(endpoint: EndpointConfig): Poster = {
    val httpClient = makeHttpClient(keystoreCertCollection, endpoint)

    Poster(endpoint.url.toString, httpClient)
  }
}