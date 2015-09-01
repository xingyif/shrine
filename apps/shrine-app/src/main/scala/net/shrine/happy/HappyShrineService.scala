package net.shrine.happy

import net.shrine.log.Loggable
import net.shrine.wiring.ShrineConfig

import scala.concurrent.Await
import scala.util.Try
import scala.xml.Node
import scala.xml.NodeSeq
import net.shrine.adapter.dao.AdapterDao
import net.shrine.adapter.service.AdapterRequestHandler
import net.shrine.broadcaster.{IdAndUrl, AdapterClientBroadcaster}
import net.shrine.service.dao.AuditDao
import net.shrine.client.Poster
import net.shrine.crypto.KeyStoreCertCollection
import net.shrine.crypto.Signer
import net.shrine.i2b2.protocol.pm.GetUserConfigurationRequest
import net.shrine.i2b2.protocol.pm.HiveConfig
import net.shrine.protocol.AuthenticationInfo
import net.shrine.protocol.BroadcastMessage
import net.shrine.protocol.Credential
import net.shrine.protocol.Failure
import net.shrine.protocol.NodeId
import net.shrine.protocol.Result
import net.shrine.protocol.ResultOutputType
import net.shrine.protocol.RunQueryRequest
import net.shrine.protocol.Timeout
import net.shrine.protocol.query.OccuranceLimited
import net.shrine.protocol.query.QueryDefinition
import net.shrine.protocol.query.Term
import net.shrine.util.{StackTrace, Versions, XmlUtil}
import net.shrine.ont.data.OntologyMetadata
import net.shrine.config.mappings.AdapterMappings
import net.shrine.crypto.SigningCertStrategy

/**
 * @author Bill Simons
 * @since 6/20/11
 * @see http://cbmi.med.harvard.edu
 * @see http://chip.org
 *       <p/>
 *       NOTICE: This software comes with NO guarantees whatsoever and is
 *       licensed as Lgpl Open Source
 * @see http://www.gnu.org/licenses/lgpl.html
 */
final class HappyShrineService(
  config: ShrineConfig,
  certCollection: KeyStoreCertCollection,
  signer: Signer,
  pmPoster: Poster,
  ontologyMetadata: OntologyMetadata,
  adapterMappings: Option[AdapterMappings],
  auditDaoOption: Option[AuditDao],
  adapterDaoOption: Option[AdapterDao],
  broadcasterOption: Option[AdapterClientBroadcaster],
  adapterOption: Option[AdapterRequestHandler]) extends HappyShrineRequestHandler with Loggable {

  info("Happy service initialized")

  private val notAnAdapter = "<notAnAdapter/>"
  private val notAHub = "<notAHub/>"

  private val domain = "happy"
  private val username = "happy"

  private val networkAuthn = AuthenticationInfo(domain, username, Credential("", isToken = false))

  override def keystoreReport: String = {

    val myCertId = certCollection.myCertId

    def unpack(name: Option[String]) = name.getOrElse("Unknown")

    XmlUtil.stripWhitespace {
      <keystoreReport>
        <keystoreFile>
          {config.keystoreDescriptor.file}
        </keystoreFile>
        <keystoreType>
          {config.keystoreDescriptor.keyStoreType}
        </keystoreType>
        <privateKeyAlias>
          {config.keystoreDescriptor.privateKeyAlias.getOrElse("unspecified")}
        </privateKeyAlias>{myCertId.map { myId =>
        <certId>
          <name>
            {unpack(myId.name)}
          </name>
          <serial>
            {myId.serial}
          </serial>
        </certId>
      }.getOrElse {
          <noPrivateKeysFound/>
      }}<importedCerts>
        {certCollection.ids.map { certId =>
          <cert>
            <name>
              {unpack(certId.name)}
            </name>
            <serial>
              {certId.serial}
            </serial>
          </cert>
        }}
      </importedCerts>
      </keystoreReport>
    }.toString()
  }

  private def nodeListAsXml: Iterable[Node] = config.hubConfig match {
    case None => Nil
    case Some(hubConfig) => hubConfig.downstreamNodes.map {
      case IdAndUrl(NodeId(nodeName), nodeUrl) => {
        <node>
          <name>{ nodeName }</name>
          <url>{ nodeUrl }</url>
        </node>
      }
    }
  }

  override def routingReport: String = XmlUtil.stripWhitespace {
    <downstreamNodes>
      {nodeListAsXml}
    </downstreamNodes>
  }.toString()

  override def hiveReport: String = {
    val report = for {
      adapterConfig <- config.adapterConfig
    } yield {
      val credentials = config.pmHiveCredentials

      val pmRequest = GetUserConfigurationRequest(credentials.toAuthenticationInfo)

      val response = pmPoster.post(pmRequest.toI2b2String)

      HiveConfig.fromI2b2(response.body).toXmlString
    }

    report.getOrElse(notAnAdapter)
  }

  private def failureToXml(failure: Failure): NodeSeq = {
    <failure>
      <origin>{ failure.origin }</origin>
      <description>{ StackTrace.stackTraceAsString(failure.cause) }</description>
    </failure>
  }

  private def timeoutToXml(timeout: Timeout): NodeSeq = {
    <timeout>
      <origin>{ timeout.origin }</origin>
    </timeout>
  }

  override def networkReport: String = {
    val report = for {
      hubConfig <- config.hubConfig
      broadcaster <- broadcasterOption
    } yield {
      val message = newBroadcastMessageWithRunQueryRequest

      val multiplexer = broadcaster.broadcast(message)

      import scala.concurrent.duration._

      val responses = Await.result(multiplexer.responses, hubConfig.maxQueryWaitTime).toSeq

      val failures = responses.collect { case f: Failure => f }

      val timeouts = responses.collect { case t: Timeout => t }

      val validResults = responses.collect { case r: Result => r }

      val noProblems = failures.isEmpty && timeouts.isEmpty

      XmlUtil.stripWhitespace {
        <net>
          <shouldQuerySelf>
            {hubConfig.shouldQuerySelf}
          </shouldQuerySelf>
          <downstreamNodes>
            {nodeListAsXml}
          </downstreamNodes>
          <noProblems>
            {noProblems}
          </noProblems>
          <expectedResultCount>
            {broadcaster.destinations.size}
          </expectedResultCount>
          <validResultCount>
            {validResults.size}
          </validResultCount>
          <failureCount>
            {failures.size}
          </failureCount>
          <timeoutCount>
            {timeouts.size}
          </timeoutCount>{nodeListAsXml}{failures.map(failureToXml)}{timeouts.map(timeoutToXml)}
        </net>
      }.toString()
    }

    report.getOrElse(notAHub)
  }

  private def newRunQueryRequest(authn: AuthenticationInfo): RunQueryRequest = {
    val queryDefinition = QueryDefinition("PDD", OccuranceLimited(1, Term(config.adapterStatusQuery)))

    import scala.concurrent.duration._

    RunQueryRequest(
      "happyProject",
      3.minutes,
      authn,
      BroadcastMessage.Ids.next,
      None,
      Set(ResultOutputType.PATIENT_COUNT_XML),
      queryDefinition)
  }

  private def newBroadcastMessageWithRunQueryRequest: BroadcastMessage = {
    val req = newRunQueryRequest(networkAuthn)

    signer.sign(BroadcastMessage(req.networkQueryId, networkAuthn, req), SigningCertStrategy.Attach)
  }

  override def adapterReport: String = {
    val report = for {
      adapterRequestHandler <- adapterOption
    } yield {
      val message = newBroadcastMessageWithRunQueryRequest

      import scala.concurrent.duration._

      val (resultAttempt: Try[Result], elapsed: Duration) = {
        val start = System.currentTimeMillis

        val attempt = Try(adapterRequestHandler.handleRequest(message))

        val end = System.currentTimeMillis

        (attempt, (end - start).milliseconds)
      }

      XmlUtil.stripWhitespace {
        <adapter>
          {resultAttempt match {
          case scala.util.Failure(cause) => failureToXml(Failure(NodeId("Local"), cause))
          case scala.util.Success(Result(origin, elapsed, response)) => {
            <result>
              <description>
                {origin}
              </description>
              <elapsed>
                {elapsed}
              </elapsed>
              <response>
                {response.toXml}
              </response>
            </result>
          }
        }}
        </adapter>
      }.toString()
    }

    report.getOrElse(notAnAdapter)
  }

  override def auditReport: String = {

    val report = for {
      auditDao <- auditDaoOption
    } yield {
      val recentEntries = auditDao.findRecentEntries(10)

      XmlUtil.stripWhitespace {
        <recentAuditEntries>
          {recentEntries map { entry =>
          <entry>
            <id>
              {entry.id}
            </id>
            <time>
              {entry.time}
            </time>
            <username>
              {entry.username}
            </username>
          </entry>
        }}
        </recentAuditEntries>
      }.toString()
    }

    report.getOrElse(notAHub)
  }

  override def queryReport: String = {
    val report = for {
      adapterDao <- adapterDaoOption
    } yield {
      val recentQueries = adapterDao.findRecentQueries(10)

      XmlUtil.stripWhitespace {
        <recentQueries>
          {recentQueries.map { query =>
          <query>
            <id>
              {query.networkId}
            </id>
            <date>
              {query.dateCreated}
            </date>
            <name>
              {query.name}
            </name>
          </query>
        }}
        </recentQueries>
      }.toString()
    }

    report.getOrElse(notAnAdapter)
  }

  override def versionReport: String = XmlUtil.stripWhitespace {
    <versionInfo>
      <shrineVersion>
        {Versions.version}
      </shrineVersion>
      <ontologyVersion>
        {ontologyMetadata.ontologyVersion}
      </ontologyVersion>
      <adapterMappingsVersion>
        {adapterMappings.map(_.version).getOrElse("No adapter mappings present")}
      </adapterMappingsVersion>
      <scmRevision>
        {Versions.scmRevision}
      </scmRevision>
      <scmBranch>
        {Versions.scmBranch}
      </scmBranch>
      <buildDate>
        {Versions.buildDate}
      </buildDate>
    </versionInfo>
  }.toString()

  override def all: String = {
    s"<all>$versionReport$keystoreReport$routingReport$hiveReport$networkReport$adapterReport$auditReport$queryReport</all>"
  }
}
