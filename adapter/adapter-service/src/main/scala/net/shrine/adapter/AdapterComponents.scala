package net.shrine.adapter

import scala.collection.JavaConverters._
import com.typesafe.config.Config
import net.shrine.adapter.dao.{AdapterDao, I2b2AdminDao}
import net.shrine.adapter.dao.squeryl.{SquerylAdapterDao, SquerylI2b2AdminDao}
import net.shrine.adapter.dao.squeryl.tables.Tables
import net.shrine.adapter.service.{AdapterService, I2b2AdminService}
import net.shrine.adapter.translators.{ExpressionTranslator, QueryDefinitionTranslator}
import net.shrine.client.{EndpointConfig, Poster}
import net.shrine.config.mappings.{AdapterMappings, AdapterMappingsSource, ClasspathFormatDetectingAdapterMappingsSource}
import net.shrine.crypto.{DefaultSignerVerifier, KeyStoreCertCollection}
import net.shrine.dao.squeryl.SquerylInitializer
import net.shrine.protocol.{HiveCredentials, NodeId, RequestType, ResultOutputType}
import net.shrine.config.{ConfigExtensions, DurationConfigParser}
import net.shrine.crypto2.{BouncyKeyStoreCollection, SignerVerifierAdapter}
import net.shrine.log.Log

import scala.concurrent.duration.Duration

/**
  * All the parts required for an adapter.
  *
  * @author david 
  * @since 1.22
  */
case class AdapterComponents(
                              adapterService: AdapterService,
                              i2b2AdminService: I2b2AdminService,
                              adapterDao: AdapterDao,
                              adapterMappings: AdapterMappings,
                              lastModified: Long
                            )

object AdapterComponents {
  //todo try and trim this argument list back
  def apply(adapterConfig: Config,
            certCollection: BouncyKeyStoreCollection,
            squerylInitializer: SquerylInitializer,
            breakdownTypes: Set[ResultOutputType],
            crcHiveCredentials: HiveCredentials,
            signerVerifier: SignerVerifierAdapter,
            pmPoster: Poster,
            nodeId: NodeId):
  AdapterComponents = {
    val crcEndpoint: EndpointConfig = adapterConfig.getConfigured("crcEndpoint",EndpointConfig(_))

    val crcPoster: Poster = Poster(certCollection,crcEndpoint)

    val squerylAdapterTables: Tables = new Tables

    val adapterDao: AdapterDao = new SquerylAdapterDao(squerylInitializer, squerylAdapterTables)(breakdownTypes)

    //NB: Is i2b2HiveCredentials.projectId the right project id to use?
    val i2b2AdminDao: I2b2AdminDao = new SquerylI2b2AdminDao(crcHiveCredentials.projectId, squerylInitializer, squerylAdapterTables)

    val adapterMappingsFile = adapterConfig.getString("adapterMappingsFileName")
    val adapterMappingsSource: AdapterMappingsSource = ClasspathFormatDetectingAdapterMappingsSource(adapterMappingsFile)

    //NB: Fail fast
    val adapterMappings: AdapterMappings = adapterMappingsSource.load(adapterMappingsFile).get

    val expressionTranslator: ExpressionTranslator = ExpressionTranslator(adapterMappings)

    val queryDefinitionTranslator: QueryDefinitionTranslator = new QueryDefinitionTranslator(expressionTranslator)

    val doObfuscation = adapterConfig.getBoolean("setSizeObfuscation")
    val collectAdapterAudit = adapterConfig.getBoolean("audit.collectAdapterAudit")

    val botCountTimeThresholds: Seq[(Long, Duration)] = {
      import scala.concurrent.duration._
      val countsAndMilliseconds: Seq[Config] = adapterConfig.getConfig("botDefense").getConfigList("countsAndMilliseconds").asScala
      countsAndMilliseconds.map(pairConfig => (pairConfig.getLong("count"),pairConfig.getLong("milliseconds").milliseconds))
    }

    val obfuscator:Obfuscator = adapterConfig.getConfigured("obfuscation",Obfuscator(_))

    Log.info(s"obfuscator is $obfuscator")

    val runQueryAdapter = RunQueryAdapter(
      poster = crcPoster,
      dao = adapterDao,
      hiveCredentials = crcHiveCredentials,
      conceptTranslator = queryDefinitionTranslator,
      adapterLockoutAttemptsThreshold = adapterConfig.getInt("adapterLockoutAttemptsThreshold"),
      doObfuscation = doObfuscation,
      runQueriesImmediately = adapterConfig.getOption("immediatelyRunIncomingQueries", _.getBoolean).getOrElse(true), //todo use reference.conf
      breakdownTypes = breakdownTypes,
      collectAdapterAudit = collectAdapterAudit,
      botCountTimeThresholds = botCountTimeThresholds,
      obfuscator = obfuscator
    )

    val readInstanceResultsAdapter: Adapter = new ReadInstanceResultsAdapter(
      poster = crcPoster,
      hiveCredentials = crcHiveCredentials,
      dao = adapterDao,
      doObfuscation = doObfuscation,
      breakdownTypes = breakdownTypes,
      collectAdapterAudit = collectAdapterAudit,
      obfuscator = obfuscator
    )

    val readQueryResultAdapter: Adapter = new ReadQueryResultAdapter(
      crcPoster,
      crcHiveCredentials,
      adapterDao,
      doObfuscation,
      breakdownTypes,
      collectAdapterAudit,
      obfuscator = obfuscator
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
      adapterMappings = adapterMappings,
      lastModified = adapterMappingsSource.lastModified)
  }
}