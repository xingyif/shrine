package net.shrine.utilities.scanner

import java.io.File
import java.io.FileInputStream

import scala.concurrent.duration.Duration
import SingleThreadExecutionContext.Implicits.executionContext
import net.shrine.adapter.client.RemoteAdapterClient
import net.shrine.broadcaster.AdapterClientBroadcaster
import net.shrine.broadcaster.BroadcastAndAggregationService
import net.shrine.broadcaster.InJvmBroadcasterClient
import net.shrine.broadcaster.NodeHandle
import net.shrine.broadcaster.SigningBroadcastAndAggregationService
import net.shrine.client.JerseyHttpClient
import net.shrine.client.Poster
import net.shrine.config.mappings.AdapterMappingsSource
import net.shrine.config.mappings.FileSystemFormatDetectingAdapterMappingsSource
import net.shrine.crypto.DefaultSignerVerifier
import net.shrine.crypto.KeyStoreCertCollection
import net.shrine.crypto.TrustParam.AcceptAllCerts
import net.shrine.hms.authentication.EcommonsPmAuthenticator
import net.shrine.ont.data.OntologyDao
import net.shrine.ont.data.ShrineSqlOntologyDao
import net.shrine.protocol.NodeId
import net.shrine.util.Versions
import net.shrine.crypto.SigningCertStrategy
import net.shrine.broadcaster.dao.HubDao
import net.shrine.crypto2.{BouncyKeyStoreCollection, SignerVerifierAdapter}
import net.shrine.protocol.query.QueryDefinition
import net.shrine.protocol.AuthenticationInfo
import net.shrine.protocol.SingleNodeResult


/**
 * @author clint
 * @since Mar 6, 2013
 */
final class ScannerModule(args: Seq[String]) {
  val commandLineProps = CommandLineScannerConfigParser(args)

  def showVersionToggleEnabled = commandLineProps.shouldShowVersion

  def showHelpToggleEnabled = commandLineProps.shouldShowHelp

  lazy val config = ClasspathAndCommandLineScannerConfigSource.config(commandLineProps)

  private lazy val scanner = {
    val client: ScannerClient = {
      
      //TODO: MAKE THIS CONFIGURABLE?
      val adapterTimeout = Duration.Inf
      
      //TODO: MAKE THIS CONFIGURABLE?
      val pmTimeout = Duration.Inf
      
      val poster = Poster(config.shrineUrl, JerseyHttpClient(AcceptAllCerts, adapterTimeout))
      
      val destinations = Set(NodeHandle(NodeId(config.shrineUrl), RemoteAdapterClient(NodeId(config.shrineUrl),poster, config.breakdownTypes)))

      val certCollection = BouncyKeyStoreCollection.fromFileRecoverWithClassPath(config.keystoreDescriptor)
      
      val signer = SignerVerifierAdapter(certCollection)
      
      val doesNothingHubDao: HubDao = new HubDao {
        override def inTransaction[T](f: => T): T = f
  
        override def logOutboundQuery(networkQueryId: Long, networkAuthn: AuthenticationInfo, queryDef: QueryDefinition): Unit = ()
  
        override def logQueryResult(networkQueryId: Long, result: SingleNodeResult): Unit = ()
      }
      
      val broadcastService: BroadcastAndAggregationService = SigningBroadcastAndAggregationService(InJvmBroadcasterClient(AdapterClientBroadcaster(destinations, doesNothingHubDao)), signer, SigningCertStrategy.Attach)

      val pmEndpoint = config.pmUrl
      
      val pmHttpClient = JerseyHttpClient(AcceptAllCerts, pmTimeout)
      
      val authenticator = EcommonsPmAuthenticator(Poster(pmEndpoint, pmHttpClient))
      
      new BroadcastServiceScannerClient(config.projectId, config.authorization, broadcastService, authenticator, executionContext)
    }

    val adapterMappingsSource: AdapterMappingsSource = FileSystemFormatDetectingAdapterMappingsSource(config.adapterMappingsFile)

    val ontologyDao: OntologyDao = new ShrineSqlOntologyDao(new FileInputStream(config.ontologySqlFile))

    new Scanner(
      config.maxTimeToWaitForResults,
      config.reScanTimeout,
      adapterMappingsSource,
      ontologyDao,
      client)
  }

  def scan(): ScanResults = {
    try { scanner.scan() }
    finally { SingleThreadExecutionContext.shutdown() }
  }
}

object ScannerModule {
  def printVersionInfo() {
    println(s"Shrine Scanner version: ${Versions.version}")
    println(s"Built on ${Versions.buildDate}")
    println(s"SCM branch: ${Versions.scmBranch}")
    println(s"SCM revision: ${Versions.scmRevision}")
    println()
  }

  def main(args: Array[String]) {

    val appName = "Shrine Scanner"
    
    val scannerModule = new ScannerModule(args)

    if (scannerModule.showVersionToggleEnabled) {
      scannerModule.commandLineProps.showVersionAndExit(appName)
    }

    if (scannerModule.showHelpToggleEnabled) {
      println("Usage: scanner [options]")

      scannerModule.commandLineProps.showHelpAndExit(appName)
    }

    val outputFile = new File(scannerModule.config.outputFile)

    val command = Output.to(outputFile)

    val scanResults = scannerModule.scan()

    command(scanResults)
  }
}