package net.shrine.status

import java.security.Security
import java.security.cert.X509Certificate
import java.util.Date
import javax.net.ssl.{KeyManager, SSLContext, X509TrustManager}
import javax.ws.rs.core.{MediaType, Response}
import javax.ws.rs.{GET, Path, Produces, WebApplicationException}

import akka.actor.ActorSystem
import akka.io.IO
import akka.util.Timeout
import com.sun.jersey.spi.container.{ContainerRequest, ContainerRequestFilter}
import com.typesafe.config.{ConfigFactory, Config => TsConfig}
import net.shrine.authorization.{QueryAuthorizationService, StewardQueryAuthorizationService}
import net.shrine.broadcaster._
import net.shrine.client.PosterOntClient
import net.shrine.config.ConfigExtensions
import net.shrine.crypto._
import net.shrine.log.{Log, Loggable}
import net.shrine.ont.data.OntClientOntologyMetadata
import net.shrine.problem.{AbstractProblem, ProblemSources}
import net.shrine.protocol._
import net.shrine.protocol.query.{OccuranceLimited, QueryDefinition, Term}
import net.shrine.serialization.NodeSeqSerializer
import net.shrine.spray._
import net.shrine.util.{PeerToPeerModel, SingleHubModel, Versions}
import net.shrine.wiring.ShrineOrchestrator
import org.json4s.native.Serialization
import org.json4s.{DefaultFormats, Formats}
import spray.can.Http
import spray.can.Http.{HostConnectorInfo, HostConnectorSetup}
import spray.client.pipelining._
import spray.http.{ContentType, ContentTypes, FormData, HttpCharsets, HttpEntity, HttpHeaders, HttpRequest, HttpResponse, MediaTypes}
import spray.io.{ClientSSLEngineProvider, PipelineContext, SSLContextProvider}

import scala.collection.JavaConverters._
import scala.collection.immutable.{Map, Seq, Set}
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{Await, Future, duration}
import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}

/**
  * A subservice that shares internal state of the shrine servlet.
  *
  * @author david 
  * @since 12/2/15
  */
@Path("/internalstatus")
@Produces(Array(MediaType.APPLICATION_JSON))
case class StatusJaxrs(shrineConfig: TsConfig) extends Loggable {

  implicit def json4sFormats: Formats = DefaultFormats + new NodeSeqSerializer

  @GET
  @Path("version")
  def version: String = {
    val version = Version("changeMe")
    val versionString = Serialization.write(version)
    versionString
  }

  @GET
  @Path("config")
  def config: String = {
    //todo probably better to reach out and grab the config from ManuallyWiredShrineJaxrsResources once it is a singleton
    Serialization.write(Json4sConfig(shrineConfig))
  }

  @GET
  @Path("summary")
  def summary: String = {
    val summary = Summary()
    Serialization.write(summary)
  }

  @GET
  @Path("i2b2")
  def i2b2: String = {
    val i2b2 = I2b2()
    Serialization.write(i2b2)
  }

  @GET
  @Path("optionalParts")
  def optionalParts: String = {
    val optionalParts = OptionalParts()
    Serialization.write(optionalParts)
  }

  @GET
  @Path("hub")
  def hub: String = {
    val hub = Hub()
    Serialization.write(hub)
  }

  @GET
  @Path("adapter")
  def adapter: String = {
    val adapter = Adapter()
    Serialization.write(adapter)
  }

  @GET
  @Path("qep")
  def qep: String = {
    val qep = Qep()
    Serialization.write(qep)
  }

  @GET
  @Path("keystore")
  def keystore: String = {
    Serialization.write(KeyStoreReport())
  }


}

/* todo fill in later when you take the time to get the right parts in place SHRINE-1529
case class KeyStoreEntryReport(
                              alias:String,
                              commonName:String,
                              md5Signature:String
                              )
*/
case class SiteStatus(siteAlias: String, theyHaveMine: Boolean, haveTheirs: Boolean, url: String, timeOutError: Boolean = false) extends DefaultJsonSupport

case class AbbreviatedKeyStoreEntry(alias: String, cn: String, md5: String) extends DefaultJsonSupport

case class KeyStoreReport(
                           fileName: String,
                           password: String = "REDACTED",
                           privateKeyAlias: Option[String],
                           owner: Option[String],
                           issuer: Option[String],
                           expires: Date,
                           md5Signature: String,
                           sha256Signature: String,
                           caTrustedAlias: Option[String],
                           caTrustedSignature: Option[String],
                           remoteSiteStatuses: Seq[SiteStatus],
                           isHub: Boolean,
                           abbreviatedEntries: Seq[AbbreviatedKeyStoreEntry]
                           //                        keyStoreContents:List[KeyStoreEntryReport] //todo SHRINE-1529
                         )

//todo build new API for the dashboard to use to check signatures

object KeyStoreReport {
  def apply(): KeyStoreReport = {
    val keyStoreDescriptor: KeyStoreDescriptor = ShrineOrchestrator.keyStoreDescriptor
    val certCollection: BouncyKeyStoreCollection = ShrineOrchestrator.certCollection
    val maybeCaEntry: Option[KeyStoreEntry] = certCollection match {
      case DownStreamCertCollection(_, caEntry, _) => Some(caEntry)
      case HubCertCollection(_, caEntry, _)        => Some(caEntry)
      case px: PeerCertCollection                  => None
    }
    val siteStatusesPreZip = ShaVerificationService(certCollection.remoteSites.toList)
    val siteStatuses = siteStatusesPreZip.zipWithIndex

    def sortFormat(input: String): Option[String] = {
      if (input.isEmpty) None
      else {
        def isLong(str: String) = str.split('=').headOption.getOrElse(str).length > 2
        // Just an ugly sort for formatting purposes. I want The long key last, and otherwise just
        // Sort them lexicographically.
        Some(input.split(", ").sortBy(a => (isLong(a), a)).mkString(", "))
      }
    }

    lazy val blockForSiteStatuses = siteStatuses.map(fut => Try(Await.result(fut._1, new FiniteDuration(10, duration.SECONDS))) match {
      case Success(Some(status)) => status
      case Success(None)         => Log.warn("There was an issue with the verifySignature endpoint, check that we have network connectivity")
        SiteStatus(certCollection.remoteSites(fut._2).alias, false, false, "", true)
      case Failure(exc)          => Log.warn("We timed out while trying to connect to the verifySignature endpoint, please check network connectivity")
        SiteStatus(certCollection.remoteSites(fut._2).alias, false, false, "", true)
    })

    new KeyStoreReport(
      fileName = keyStoreDescriptor.file,
      privateKeyAlias = keyStoreDescriptor.privateKeyAlias,
      owner = sortFormat(certCollection.myEntry.cert.getSubjectDN.getName),
      issuer = sortFormat(certCollection.myEntry.cert.getIssuerDN.getName),
      expires = certCollection.myEntry.cert.getNotAfter,
      md5Signature = UtilHasher.encodeCert(certCollection.myEntry.cert, "MD5"),
      sha256Signature = UtilHasher.encodeCert(certCollection.myEntry.cert, "SHA-256"),
      //todo sha1 signature if needed
      caTrustedAlias = maybeCaEntry.map(_.aliases.first),
      caTrustedSignature = maybeCaEntry.map(entry => UtilHasher.encodeCert(entry.cert, "MD5")),
      remoteSiteStatuses = blockForSiteStatuses,
      isHub = keyStoreDescriptor.trustModel == SingleHubModel(true),
      abbreviatedEntries = certCollection.allEntries.map(entry => AbbreviatedKeyStoreEntry(
        entry.aliases.first,
        entry.commonName.getOrElse("No common name"),
        UtilHasher.encodeCert(entry.cert, "MD5"))).toList

      //      keyStoreContents = certCollection.caCerts.zipWithIndex.map((cert: ((Principal, X509Certificate), Int)) => KeyStoreEntryReport(keyStoreDescriptor.caCertAliases(cert._2),cert._1._1.getName,toMd5(cert._1._2))).to[List]
    )
  }
}

case class I2b2(pmUrl: String,
                crcUrl: Option[String],
                ontUrl: String,
                i2b2Domain: String,
                username: String,
                crcProject: String,
                ontProject: String)

object I2b2 {
  def apply(): I2b2 = new I2b2(
    pmUrl = ShrineOrchestrator.pmPoster.url,
    crcUrl = ShrineOrchestrator.adapterComponents.map(_.i2b2AdminService.crcUrl),
    ontUrl = "", //todo. Grab from HiveConfig?
    i2b2Domain = ShrineOrchestrator.crcHiveCredentials.domain,
    username = ShrineOrchestrator.crcHiveCredentials.username,
    crcProject = ShrineOrchestrator.crcHiveCredentials.projectId,
    ontProject = ShrineOrchestrator.ontologyMetadata.client match {
      case client: PosterOntClient => client.hiveCredentials.projectId
      case _                       => ""
    }
  )
}

case class DownstreamNode(name: String, url: String)

// Replaces StewardQueryAuthorizationService so that we never transmit a password
case class Steward(stewardBaseUrl: String, qepUsername: String, password: String = "REDACTED")

case class Qep(
                maxQueryWaitTimeMillis: Long,
                create: Boolean,
                attachSigningCert: Boolean,
                authorizationType: String,
                includeAggregateResults: Boolean,
                authenticationType: String,
                steward: Option[Steward],
                broadcasterUrl: Option[String],
                trustModel: String,
                trustModelIsHub: Boolean
              )

object Qep {
  val key = "shrine.queryEntryPoint."

  import ShrineOrchestrator.queryEntryPointComponents

  def apply(): Qep = new Qep(
    maxQueryWaitTimeMillis = queryEntryPointComponents.fold(0L)(_.i2b2Service.queryTimeout.toMicros),
    create = queryEntryPointComponents.isDefined,
    attachSigningCert = queryEntryPointComponents.fold(false)(_.i2b2Service.broadcastAndAggregationService.attachSigningCert),
    authorizationType = queryEntryPointComponents.fold("")(_.i2b2Service.authorizationService.getClass.getSimpleName),
    includeAggregateResults = queryEntryPointComponents.fold(false)(_.i2b2Service.includeAggregateResult),
    authenticationType = queryEntryPointComponents.fold("")(_.i2b2Service.authenticator.getClass.getSimpleName),
    steward = queryEntryPointComponents.flatMap(qec => checkStewardAuthorization(qec.shrineService.authorizationService)),
    broadcasterUrl = queryEntryPointComponents.flatMap(qec => checkBroadcasterUrl(qec.i2b2Service.broadcastAndAggregationService)),
    trustModel = ShrineOrchestrator.keyStoreDescriptor.trustModel.description,
    trustModelIsHub = ShrineOrchestrator.keyStoreDescriptor.trustModel match {
      case sh: SingleHubModel => true
      case PeerToPeerModel    => false
    }
  )

  def checkStewardAuthorization(auth: QueryAuthorizationService): Option[Steward] = auth match {
    case sa: StewardQueryAuthorizationService => Some(Steward(sa.stewardBaseUrl.toString, sa.qepUserName))
    case _                                    => None
  }

  //TODO: Double check with Dave that this is the right url
  def checkBroadcasterUrl(broadcaster: BroadcastAndAggregationService): Option[String] = broadcaster match {
    case a: HubBroadcastAndAggregationService => a.broadcasterClient match {
      case PosterBroadcasterClient(poster, _) => Some(poster.url)
      case _                                  => None
    }
    case _                                    => None
  }
}

object DownstreamNodes {
  def get(): Seq[DownstreamNode] = {
    ShrineOrchestrator.hubComponents.fold(Seq.empty[DownstreamNode])(_.broadcastDestinations.map(DownstreamNode(_)).to[Seq])
  }
}

object DownstreamNode {
  def apply(nodeHandle: NodeHandle): DownstreamNode = new DownstreamNode(
    nodeHandle.nodeId.name,
    nodeHandle.client.url.map(_.toString).getOrElse("not applicable"))
}

case class Adapter(crcEndpointUrl: String,
                   setSizeObfuscation: Boolean,
                   adapterLockoutAttemptsThreshold: Int,
                   adapterMappingsFilename: Option[String],
                   adapterMappingsDate: Option[Long]
                  )

object
Adapter {
  def apply(): Adapter = {
    val crcEndpointUrl = ShrineOrchestrator.adapterComponents.fold("")(_.i2b2AdminService.crcUrl)
    val setSizeObfuscation = ShrineOrchestrator.adapterComponents.fold(false)(_.i2b2AdminService.obfuscate)
    val adapterLockoutAttemptsThreshold = ShrineOrchestrator.adapterComponents.fold(0)(_.i2b2AdminService.adapterLockoutAttemptsThreshold)
    val adapterMappingsFileName = mappingFileInfo.map(_._1)
    val adapterMappingsFileDate = mappingFileInfo.map(_._2)

    Adapter(crcEndpointUrl, setSizeObfuscation, adapterLockoutAttemptsThreshold, adapterMappingsFileName, adapterMappingsFileDate)
  }

  def mappingFileInfo: Option[(String, Long, String)] =
    ShrineOrchestrator.adapterComponents.map(ac => (ac.adapterMappings.source, ac.lastModified, ac.adapterMappings.version))
}

case class Hub(shouldQuerySelf: Boolean, //todo don't use this field any more. Drop it when possible
               create: Boolean,
               downstreamNodes: Seq[DownstreamNode])

object Hub {
  def apply(): Hub = {
    val shouldQuerySelf = false
    val create = ShrineOrchestrator.hubComponents.isDefined
    val downstreamNodes = DownstreamNodes.get()
    Hub(shouldQuerySelf, create, downstreamNodes)
  }
}


case class OptionalParts(isHub: Boolean,
                         stewardEnabled: Boolean,
                         shouldQuerySelf: Boolean, //todo don't use this field any more. Drop it when possible
                         downstreamNodes: Seq[DownstreamNode])

object OptionalParts {
  def apply(): OptionalParts = {
    OptionalParts(
      ShrineOrchestrator.hubComponents.isDefined,
      ShrineOrchestrator.queryEntryPointComponents.fold(false)(_.shrineService.authorizationService.isInstanceOf[StewardQueryAuthorizationService]),
      shouldQuerySelf = false,
      DownstreamNodes.get()
    )
  }
}

case class Summary(
                    isHub: Boolean,
                    shrineVersion: String,
                    shrineBuildDate: String,
                    ontologyVersion: String,
                    ontologyVersionTerm: String,
                    ontologyTerm: String,
                    queryResult: Option[SingleNodeResult],
                    adapterMappingsFileName: Option[String],
                    adapterMappingsDate: Option[Long],
                    adapterOk: Boolean,
                    keystoreOk: Boolean,
                    hubOk: Boolean,
                    qepOk: Boolean
                  )

object Summary {

  val term = Term(ShrineOrchestrator.shrineConfig.getString("networkStatusQuery"))

  def runQueryRequest: BroadcastMessage = {
    val domain = "happy"
    val username = "happy"

    val networkAuthn = AuthenticationInfo(domain, username, Credential("", isToken = false))

    val queryDefinition = QueryDefinition("TestQuery", OccuranceLimited(1, term))
    import scala.concurrent.duration._
    val req = RunQueryRequest(
      "happyProject",
      3.minutes,
      networkAuthn,
      None,
      None,
      Set(ResultOutputType.PATIENT_COUNT_XML),
      queryDefinition)


    ShrineOrchestrator.signerVerifier.sign(BroadcastMessage(req.networkQueryId, networkAuthn, req), SigningCertStrategy.Attach)
  }

  def apply(): Summary = {

    val message = runQueryRequest

    val queryResult: Option[SingleNodeResult] = ShrineOrchestrator.adapterService.map { adapterService =>

      import scala.concurrent.duration._

      val start = System.currentTimeMillis
      val resultAttempt: Try[Result] = Try(adapterService.handleRequest(message))
      val end = System.currentTimeMillis
      val elapsed = (end - start).milliseconds

      resultAttempt match {
        case scala.util.Success(result)    => result
        case scala.util.Failure(throwable) => FailureResult(NodeId("Local"), throwable)
      }
    }

    val adapterOk = queryResult.fold(true) {
      case r: Result        => true
      case f: FailureResult => false
    }

    val hubOk = ShrineOrchestrator.hubComponents.fold(true) { hubComponents =>
      val maxQueryWaitTime = hubComponents.broadcasterMultiplexerService.maxQueryWaitTime
      val broadcaster: Broadcaster = hubComponents.broadcasterMultiplexerService.broadcaster
      val message = runQueryRequest
      val triedMultiplexer = Try(broadcaster.broadcast(message))
      //todo just use fold()() in scala 2.12
      triedMultiplexer.toOption.fold(false) { multiplexer =>
        val responses = Await.result(multiplexer.responses, maxQueryWaitTime).toSeq
        val failures = responses.collect { case f: FailureResult => f }
        val timeouts = responses.collect { case t: Timeout => t }
        val validResults = responses.collect { case r: Result => r }

        failures.isEmpty && timeouts.isEmpty && (validResults.size == broadcaster.destinations.size)
      }
    }

    val adapterMappingInfo = Adapter.mappingFileInfo

    val ontologyVersion = try {
      ShrineOrchestrator.ontologyMetadata.ontologyVersion
    }
    catch {
      case NonFatal(x) =>
        Log.info("Problem while getting ontology version", x)
        s"Unavailable due to: ${x.getMessage}"
    }

    Summary(
      isHub = ShrineOrchestrator.hubComponents.isDefined,
      shrineVersion = Versions.version,
      shrineBuildDate = Versions.buildDate,
      //todo in scala 2.12, do better
      ontologyVersion = ontologyVersion,
      ontologyVersionTerm = OntClientOntologyMetadata.versionContainerTerm,
      ontologyTerm = term.value,
      queryResult = queryResult,
      adapterMappingsFileName = adapterMappingInfo.map(_._1),
      adapterMappingsDate = adapterMappingInfo.map(_._2),
      adapterOk = adapterOk,
      keystoreOk = true, //todo something for this
      hubOk = hubOk,
      qepOk = true //todo something for this
    )
  }
}

case class Version(version: String)

//todo SortedMap when possible
case class Json4sConfig(keyValues: Map[String, String])

object Json4sConfig {
  def isPassword(key: String): Boolean = {
    if (key.toLowerCase.contains("password")) true
    else false
  }

  def apply(config: TsConfig): Json4sConfig = {
    val entries: Set[(String, String)] = config.entrySet.asScala.to[Set].map(x => (x.getKey, x.getValue.render())).filterNot(x => isPassword(x._1))
    val sortedMap: Map[String, String] = entries.toMap
    Json4sConfig(sortedMap)
  }
}

class PermittedHostOnly extends ContainerRequestFilter {

  //todo generalize for happy, too
  //todo for tomcat 8 see https://jersey.java.net/documentation/latest/filters-and-interceptors.html for a cleaner version
  //shell code from http://stackoverflow.com/questions/17143514/how-to-add-custom-response-and-abort-request-in-jersey-1-11-filters

  //how to apply in http://stackoverflow.com/questions/4358213/how-does-one-intercept-a-request-during-the-jersey-lifecycle
  override def filter(requestContext: ContainerRequest): ContainerRequest = {
    val hostOfOrigin = requestContext.getBaseUri.getHost
    val shrineConfig: TsConfig = ShrineOrchestrator.config
    val permittedHostOfOrigin: String = shrineConfig.getOption("shrine.status.permittedHostOfOrigin", _.getString).getOrElse("localhost")

    val path = requestContext.getPath

    //happy and internalstatus API calls must come from the same host as tomcat is running on (hopefully the dashboard servlet).
    // todo access to the happy service permitted for SHRINE 1.21 per SHRINE-1366
    // restrict access to happy service when database work resumes as part of SHRINE-
    //       if ((path.contains("happy") || path.contains("internalstatus")) && (hostOfOrigin != permittedHostOfOrigin)) {
    if (path.contains("internalstatus") && (hostOfOrigin != permittedHostOfOrigin)) {
      val response = Response.status(Response.Status.UNAUTHORIZED).entity(s"Only available from $permittedHostOfOrigin, not $hostOfOrigin, controlled by shrine.status.permittedHostOfOrigin in shrine.conf").build()
      throw new WebApplicationException(response)
    }
    else requestContext
  }

}

object ShaVerificationService extends Loggable with DefaultJsonSupport {
  //todo: remove duplication with StewardQueryAuthorizationService
  import akka.pattern.ask
  import org.json4s.native.JsonMethods.parseOpt
  import system.dispatcher

  // execution context for futures
  implicit val system = ActorSystem("AuthorizationServiceActors", ConfigFactory.load("shrine")) //todo use shrine's config

  val certCollection = ShrineOrchestrator.certCollection

  def sendHttpRequest(httpRequest: HttpRequest): Future[HttpResponse] = {
    implicit val timeout: Timeout = Timeout.durationToTimeout(new FiniteDuration(10, duration.SECONDS)) //10 seconds

    implicit def json4sFormats: Formats = DefaultFormats

    implicit def trustfulSslContext: SSLContext = {
      object BlindFaithX509TrustManager extends X509TrustManager {
        def checkClientTrusted(chain: Array[X509Certificate], authType: String) = info(s"Client asked BlindFaithX509TrustManager to check $chain for $authType")

        def checkServerTrusted(chain: Array[X509Certificate], authType: String) = info(s"Server asked BlindFaithX509TrustManager to check $chain for $authType")

        def getAcceptedIssuers = Array[X509Certificate]()
      }

      val context = SSLContext.getInstance("TLS")
      context.init(Array[KeyManager](), Array(BlindFaithX509TrustManager), null)
      context
    }

    implicit def trustfulSslContextProvider: SSLContextProvider = {
      SSLContextProvider.forContext(trustfulSslContext)
    }

    class CustomClientSSLEngineProvider extends ClientSSLEngineProvider {
      def apply(pc: PipelineContext) = ClientSSLEngineProvider.default(trustfulSslContextProvider).apply(pc)
    }

    implicit def sslEngineProvider: ClientSSLEngineProvider = new CustomClientSSLEngineProvider

    val responseFuture: Future[HttpResponse] = for {
      HostConnectorInfo(hostConnector, _) <- {
        val hostConnectorSetup = new HostConnectorSetup(httpRequest.uri.authority.host.address,
          httpRequest.uri.authority.port,
          sslEncryption = httpRequest.uri.scheme == "https")(
          sslEngineProvider = sslEngineProvider)

        IO(Http) ask hostConnectorSetup
      }
      response <- sendReceive(hostConnector).apply(httpRequest)
      _ <- hostConnector ask Http.CloseAll
    } yield response

    responseFuture
  }

  type MaybeSiteStatus = Future[Option[SiteStatus]]

  def apply(sites: Seq[RemoteSite]): Seq[MaybeSiteStatus] = sites.map(curl)


  def curl(site: RemoteSite): MaybeSiteStatus = {
    val shaEntry = certCollection match {
      case HubCertCollection(_, caEntry, _) => caEntry
      case PeerCertCollection(my, _, _) => my
      case DownStreamCertCollection(_, caEntry, _) => caEntry
    }
    val sha256 = UtilHasher.encodeCert(shaEntry.cert, "SHA-256")
    implicit val formats = org.json4s.DefaultFormats
    val request = Post(s"https://${site.url}:${site.port}/shrine-dashboard/status/verifySignature")
      .withEntity( // For some reason, FormData isn't producing the correct HTTP call, so we do it manually
        HttpEntity.apply(
          ContentType(
            MediaTypes.`application/x-www-form-urlencoded`,
            HttpCharsets.`UTF-8`),
          s"sha256=$sha256"))

    for {response <- sendHttpRequest(request)
         rawResponse = new String(response.entity.data.toByteArray)
         status = parseOpt(rawResponse).fold(handleError(rawResponse))(_.extractOpt[ShaResponse] match {
           case Some(ShaResponse(ShaResponse.badFormat, false)) =>
             error(s"Somehow, this client is sending an incorrectly formatted SHA256 signature to the dashboard. Offending sig: $sha256")
             None
           case Some(ShaResponse(sha, true))                 => Some(SiteStatus(site.alias, theyHaveMine = true, haveTheirs = doWeHaveCert(sha), site.url))
           case Some(ShaResponse(sha, false))                => Some(SiteStatus(site.alias, theyHaveMine = false, haveTheirs = doWeHaveCert(sha), site.url))
           case None                                            =>
             InvalidVerifySignatureResponse(rawResponse)
             None
         })} yield status
  }

  def doWeHaveCert(sha256: String): Boolean = UtilHasher(certCollection).handleSig(sha256).found

  def handleError(response: String): Option[SiteStatus] = {
    InvalidVerifySignatureResponse(response)
    None
  }

  case class InvalidVerifySignatureResponse(response: String) extends AbstractProblem(ProblemSources.ShrineApp) {
    override def summary: String = "The client for handling certificate diagnostic across Dashboards in the Status Service received an invalid response from verifySignature"

    override def description: String = s"verifySig produced the invalid response `$response`"
  }

  def main(args: Array[String]): Unit = {
    val entry = CertificateCreator.createSelfSignedCertEntry("boo", "der", "derder")

    println(Await.result(curl(RemoteSite("shrine-dev1.catalyst", Some(entry), "alias", "6443")), new FiniteDuration(10, duration.SECONDS)))
  }
}
