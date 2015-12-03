package net.shrine.wiring

import com.typesafe.config.{Config, ConfigFactory}
import net.shrine.adapter.Adapter
import net.shrine.adapter.AdapterMap
import net.shrine.adapter.DeleteQueryAdapter
import net.shrine.adapter.FlagQueryAdapter
import net.shrine.adapter.ReadInstanceResultsAdapter
import net.shrine.adapter.ReadPreviousQueriesAdapter
import net.shrine.adapter.ReadQueryDefinitionAdapter
import net.shrine.adapter.ReadQueryResultAdapter
import net.shrine.adapter.ReadTranslatedQueryDefinitionAdapter
import net.shrine.adapter.RenameQueryAdapter
import net.shrine.adapter.RunQueryAdapter
import net.shrine.adapter.UnFlagQueryAdapter
import net.shrine.adapter.dao.AdapterDao
import net.shrine.adapter.dao.I2b2AdminDao
import net.shrine.adapter.dao.squeryl.SquerylAdapterDao
import net.shrine.adapter.dao.squeryl.SquerylI2b2AdminDao
import net.shrine.adapter.dao.squeryl.tables.{Tables => AdapterTables}
import net.shrine.adapter.service.AdapterRequestHandler
import net.shrine.adapter.service.AdapterResource
import net.shrine.adapter.service.AdapterService
import net.shrine.adapter.service.I2b2AdminResource
import net.shrine.adapter.service.I2b2AdminService
import net.shrine.adapter.translators.ExpressionTranslator
import net.shrine.adapter.translators.QueryDefinitionTranslator
import net.shrine.authentication.Authenticator
import net.shrine.authorization.QueryAuthorizationService
import net.shrine.broadcaster.AdapterClientBroadcaster
import net.shrine.broadcaster.BroadcastAndAggregationService
import net.shrine.broadcaster.BroadcasterClient
import net.shrine.broadcaster.InJvmBroadcasterClient
import net.shrine.broadcaster.NodeHandle
import net.shrine.broadcaster.PosterBroadcasterClient
import net.shrine.broadcaster.SigningBroadcastAndAggregationService
import net.shrine.log.Loggable
import net.shrine.service.dao.AuditDao
import net.shrine.service.dao.squeryl.SquerylAuditDao
import net.shrine.service.dao.squeryl.tables.{Tables => HubTables}
import net.shrine.broadcaster.service.BroadcasterMultiplexerResource
import net.shrine.broadcaster.service.BroadcasterMultiplexerService
import net.shrine.client.{EndpointConfig, HttpClient, JerseyHttpClient, OntClient, Poster, PosterOntClient}
import net.shrine.config.mappings.AdapterMappings
import net.shrine.config.mappings.AdapterMappingsSource
import net.shrine.config.mappings.ClasspathFormatDetectingAdapterMappingsSource
import net.shrine.crypto.DefaultSignerVerifier
import net.shrine.crypto.KeyStoreCertCollection
import net.shrine.dao.squeryl.DataSourceSquerylInitializer
import net.shrine.dao.squeryl.SquerylDbAdapterSelecter
import net.shrine.ont.data.OntClientOntologyMetadata
import net.shrine.protocol.NodeId
import net.shrine.protocol.RequestType
import net.shrine.protocol.ResultOutputType
import net.shrine.happy.HappyShrineResource
import net.shrine.happy.HappyShrineService
import net.shrine.service.I2b2BroadcastResource
import net.shrine.service.I2b2BroadcastService
import net.shrine.service.ShrineResource
import net.shrine.service.ShrineService
import net.shrine.crypto.TrustParam
import javax.sql.DataSource
import net.shrine.status.StatusJaxrs
import org.squeryl.internals.DatabaseAdapter
import net.shrine.dao.squeryl.SquerylInitializer
import net.shrine.ont.data.OntologyMetadata
import net.shrine.broadcaster.dao.HubDao
import net.shrine.broadcaster.dao.squeryl.SquerylHubDao

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
class ManuallyWiredShrineJaxrsResources(authStrategy: AuthStrategy = AuthStrategy.Default) extends ShrineJaxrsResources with Loggable {
  import ManuallyWiredShrineJaxrsResources._
  import NodeHandleSource.makeNodeHandles

  override def resources: Iterable[AnyRef] = {
    Seq(happyResource,statusJaxrs) ++ shrineResource ++ i2b2BroadcastResource ++ adapterResource ++ i2b2AdminResource ++ broadcasterMultiplexerResource
  }
  
  //Load config from file on the classpath called "shrine.conf"
  protected lazy val config: Config = ConfigFactory.load("shrine")
  protected lazy val shrineConfig: ShrineConfig = ShrineConfig(config)

  protected lazy val nodeId: NodeId = NodeId(shrineConfig.humanReadableNodeName)

  //TODO: Don't assume keystore lives on the filesystem, could come from classpath, etc
  protected lazy val shrineCertCollection: KeyStoreCertCollection = KeyStoreCertCollection.fromFile(shrineConfig.keystoreDescriptor)

  protected lazy val keystoreTrustParam: TrustParam = TrustParam.SomeKeyStore(shrineCertCollection)

  protected lazy val signerVerifier: DefaultSignerVerifier = new DefaultSignerVerifier(shrineCertCollection)

  protected lazy val dataSource: DataSource = Jndi("java:comp/env/jdbc/shrineDB").get

  protected lazy val squerylAdapter: DatabaseAdapter = SquerylDbAdapterSelecter.determineAdapter(shrineConfig.shrineDatabaseType)

  protected lazy val squerylInitializer: SquerylInitializer = new DataSourceSquerylInitializer(dataSource, squerylAdapter)

  private def makePoster = poster(shrineCertCollection) _

  private lazy val pmEndpoint: EndpointConfig = shrineConfig.pmEndpoint

  private lazy val ontEndpoint: EndpointConfig = shrineConfig.ontEndpoint

  protected lazy val pmPoster: Poster = makePoster(pmEndpoint)

  protected lazy val ontPoster: Poster = makePoster(ontEndpoint)
  
  protected lazy val breakdownTypes: Set[ResultOutputType] = shrineConfig.breakdownResultOutputTypes
  
  protected lazy val hubDao: HubDao = new SquerylHubDao(squerylInitializer, new net.shrine.broadcaster.dao.squeryl.tables.Tables)

  //todo move as much of this block as possible to the adapter project
  protected lazy val (adapterService, i2b2AdminService, adapterDao, adapterMappings) = adapterComponentsToTuple(shrineConfig.adapterConfig.map { adapterConfig =>

    val crcEndpoint: EndpointConfig = adapterConfig.crcEndpoint

    val crcPoster: Poster = makePoster(crcEndpoint)

    val squerylAdapterTables: AdapterTables = new AdapterTables

    val adapterDao: AdapterDao = new SquerylAdapterDao(squerylInitializer, squerylAdapterTables)(breakdownTypes)

    //NB: Is i2b2HiveCredentials.projectId the right project id to use?
    val i2b2AdminDao: I2b2AdminDao = new SquerylI2b2AdminDao(shrineConfig.crcHiveCredentials.projectId, squerylInitializer, squerylAdapterTables)

    val adapterMappingsSource: AdapterMappingsSource = ClasspathFormatDetectingAdapterMappingsSource(adapterConfig.adapterMappingsFileName)

    //NB: Fail fast
    val adapterMappings: AdapterMappings = adapterMappingsSource.load.get

    val expressionTranslator: ExpressionTranslator = ExpressionTranslator(adapterMappings)

    val queryDefinitionTranslator: QueryDefinitionTranslator = new QueryDefinitionTranslator(expressionTranslator)

    val doObfuscation = adapterConfig.setSizeObfuscation
    
    val runQueryAdapter = new RunQueryAdapter(
      crcPoster,
      adapterDao,
      shrineConfig.crcHiveCredentials,
      queryDefinitionTranslator,
      adapterConfig.adapterLockoutAttemptsThreshold,
      doObfuscation,
      adapterConfig.immediatelyRunIncomingQueries,
      breakdownTypes,
      collectAdapterAudit = adapterConfig.collectAdapterAudit
    )

    val readInstanceResultsAdapter: Adapter = new ReadInstanceResultsAdapter(
      crcPoster,
      shrineConfig.crcHiveCredentials,
      adapterDao,
      doObfuscation,
      breakdownTypes,
      collectAdapterAudit = adapterConfig.collectAdapterAudit
    )

    val readQueryResultAdapter: Adapter = new ReadQueryResultAdapter(
      crcPoster,
      shrineConfig.crcHiveCredentials,
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

  private val localAdapterServiceOption: Option[AdapterRequestHandler] = shrineConfig.hubConfig.flatMap(hubConfig => makeAdapterServiceOption(hubConfig.shouldQuerySelf, adapterService))
  
  private val broadcastDestinations: Option[Set[NodeHandle]] = shrineConfig.hubConfig.map(hubConfig => makeNodeHandles(keystoreTrustParam, hubConfig.maxQueryWaitTime, hubConfig.downstreamNodes, nodeId, localAdapterServiceOption, breakdownTypes))
  
  protected lazy val (shrineService, i2b2Service, auditDao) = queryEntryPointComponentsToTuple(shrineConfig.queryEntryPointConfig.map { queryEntryPointConfig =>

      val broadcasterClient: BroadcasterClient = {
        if(queryEntryPointConfig.broadcasterIsLocal) {
          //If broadcaster is local, we need a hub config
          //TODO: Enforce this when unmarshalling configs
          require(broadcastDestinations.isDefined, "Local broadcaster specified, but no downstream nodes defined")
          
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

      val authenticator: Authenticator = authStrategy.determineAuthenticator(authenticationType, pmPoster)
      
      val authorizationService: QueryAuthorizationService = authStrategy.determineQueryAuthorizationService(authorizationType, shrineConfig, authenticator)

      debug(s"authorizationService set to $authorizationService")

      QueryEntryPointComponents(
        ShrineService(
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
        I2b2BroadcastService(
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
      hubConfig <- shrineConfig.hubConfig
    } yield {
      require(broadcastDestinations.isDefined, "This node is configured to be a hub, but no downstream nodes are defined")
      
      HubComponents(AdapterClientBroadcaster(broadcastDestinations.get, hubDao))
    }
  }
  
  protected lazy val broadcasterMultiplexerService = {
    for {
      broadcaster <- broadcasterOption
      hubConfig <- shrineConfig.hubConfig
    } yield {
      BroadcasterMultiplexerService(broadcaster, hubConfig.maxQueryWaitTime)
    }
  }

  protected lazy val pmUrlString: String = shrineConfig.pmEndpoint.url.toString

  protected lazy val ontologyMetadata: OntologyMetadata = {
    import scala.concurrent.duration._

    //TODO: XXX: Un-hard-code max wait time param
    val ontClient: OntClient = new PosterOntClient(shrineConfig.ontHiveCredentials, 1.minute, ontPoster)

    new OntClientOntologyMetadata(ontClient)
  }

  //TODO: Don't assume we're an adapter with an AdapterMappings (don't call .get)
  protected lazy val happyService: HappyShrineService = {
    new HappyShrineService(
        shrineConfig, 
        shrineCertCollection, 
        signerVerifier, 
        pmPoster, 
        ontologyMetadata, 
        adapterMappings, 
        auditDao, 
        adapterDao, 
        broadcasterOption, 
        adapterService)
  }

  protected lazy val happyResource: HappyShrineResource = new HappyShrineResource(happyService)

  protected lazy val statusJaxrs: StatusJaxrs = StatusJaxrs(config)

  protected lazy val shrineResource: Option[ShrineResource] = shrineService.map(ShrineResource(_))

  protected lazy val i2b2BroadcastResource: Option[I2b2BroadcastResource] = i2b2Service.map(new I2b2BroadcastResource(_, breakdownTypes))

  protected lazy val adapterResource: Option[AdapterResource] = adapterService.map(AdapterResource(_))

  protected lazy val i2b2AdminResource: Option[I2b2AdminResource] = i2b2AdminService.map(I2b2AdminResource(_, breakdownTypes))
  
  protected lazy val broadcasterMultiplexerResource: Option[BroadcasterMultiplexerResource] = broadcasterMultiplexerService.map(BroadcasterMultiplexerResource(_))
}

object ManuallyWiredShrineJaxrsResources {
  def makeAdapterServiceOption(isQueryable: Boolean, adapterRequestHandler: Option[AdapterRequestHandler]): Option[AdapterRequestHandler] = {
    if (isQueryable) {
      require(adapterRequestHandler.isDefined, "Self-querying requested, but this node is not configured to be an adapter")

      adapterRequestHandler
    } else { None }
  }

  def makeHttpClient(keystoreCertCollection: KeyStoreCertCollection, endpoint: EndpointConfig): HttpClient = {
    import TrustParam.{ AcceptAllCerts, SomeKeyStore }

    val trustParam = if (endpoint.acceptAllCerts) AcceptAllCerts else SomeKeyStore(keystoreCertCollection)

    JerseyHttpClient(trustParam, endpoint.timeout)
  }

  private final case class AdapterComponents(adapterService: AdapterService, i2b2AdminService: I2b2AdminService, adapterDao: AdapterDao, adapterMappings: AdapterMappings)

  private final case class QueryEntryPointComponents(shrineService: ShrineService, i2b2Service: I2b2BroadcastService, auditDao: AuditDao)

  private final case class HubComponents(broadcaster: AdapterClientBroadcaster)

  //TODO: TEST
  private def adapterComponentsToTuple(option: Option[AdapterComponents]): (Option[AdapterService], Option[I2b2AdminService], Option[AdapterDao], Option[AdapterMappings]) = option match {
    case None => (None, None, None, None)
    case Some(AdapterComponents(a, b, c, d)) => (Option(a), Option(b), Option(c), Option(d))
  }
  
  //TODO: TEST
  private def queryEntryPointComponentsToTuple(option: Option[QueryEntryPointComponents]): (Option[ShrineService], Option[I2b2BroadcastService], Option[AuditDao]) = option match {
    case None => (None, None, None)
    case Some(QueryEntryPointComponents(a, b, c)) => (Option(a), Option(b), Option(c))
  }
  
  //TODO: TEST
  private def unpackHubComponents(option: Option[HubComponents]): Option[AdapterClientBroadcaster] = option.map(_.broadcaster)

  def poster(keystoreCertCollection: KeyStoreCertCollection)(endpoint: EndpointConfig): Poster = {
    val httpClient = makeHttpClient(keystoreCertCollection, endpoint)

    Poster(endpoint.url.toString, httpClient)
  }
}
