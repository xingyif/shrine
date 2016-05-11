package net.shrine.happy

import net.shrine.broadcaster.{Broadcaster, NodeHandle}
import net.shrine.crypto.SigningCertStrategy
import net.shrine.i2b2.protocol.pm.{GetUserConfigurationRequest, HiveConfig}
import net.shrine.log.Loggable
import net.shrine.protocol.query.{OccuranceLimited, QueryDefinition, Term}
import net.shrine.protocol.{AuthenticationInfo, BroadcastMessage, Credential, Failure, NodeId, Result, ResultOutputType, RunQueryRequest, Timeout}
import net.shrine.util.{StackTrace, Versions, XmlUtil}
import net.shrine.wiring.ShrineOrchestrator

import scala.concurrent.Await
import scala.util.Try
import scala.xml.{Node, NodeSeq}

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
object HappyShrineService extends HappyShrineRequestHandler with Loggable {

  info("Happy service initialized")

  private val notAnAdapter = "<notAnAdapter/>"
  private val notAHub = "<notAHub/>"

  private val domain = "happy"
  private val username = "happy"

  private val networkAuthn = AuthenticationInfo(domain, username, Credential("", isToken = false))

  override def keystoreReport: String = {

    val keystoreDescriptor = ShrineOrchestrator.keyStoreDescriptor
    val certCollection = ShrineOrchestrator.certCollection

    val myCertId = certCollection.myCertId

    def unpack(name: Option[String]) = name.getOrElse("Unknown")

    XmlUtil.stripWhitespace {
      <keystoreReport>
        <keystoreFile>{ keystoreDescriptor.file }</keystoreFile>
        <keystoreType>{ keystoreDescriptor.keyStoreType }</keystoreType>
        <privateKeyAlias>{ keystoreDescriptor.privateKeyAlias.getOrElse("unspecified") }</privateKeyAlias>
        {
          myCertId.map { myId =>
            <certId>
              <name>{ unpack(myId.name) }</name>
              <serial>{ myId.serial }</serial>
            </certId>
          }.getOrElse {
            <noPrivateKeysFound/>
          }
        }
        <importedCerts>
          {
            certCollection.ids.map { certId =>
              <cert>
                <name>{ unpack(certId.name) }</name>
                <serial>{ certId.serial }</serial>
              </cert>
            }
          }
        </importedCerts>
      </keystoreReport>
    }.toString
  }

  private def nodeListAsXml: Iterable[Node] = {

    val noneResult: Iterable[Node] = Nil
    ShrineOrchestrator.broadcasterMultiplexerService.fold(noneResult) { broadcasterMultiplexerService =>

      val broadcaster = broadcasterMultiplexerService.broadcaster
      broadcaster.destinations.map{ node:NodeHandle =>
        <node>
          <name>{ node.nodeId.name }</name>
          <url>{ node.client.url.getOrElse("").toString }</url>
        </node>
      }
    }
  }

  override def routingReport: String = XmlUtil.stripWhitespace {
    <downstreamNodes>{ nodeListAsXml }</downstreamNodes>
  }.toString

  override def hiveReport: String = {
    if(ShrineOrchestrator.shrineConfig.getBoolean("adapter.create")) {
      val credentials = ShrineOrchestrator.crcHiveCredentials
      val pmRequest = GetUserConfigurationRequest(credentials.toAuthenticationInfo)
      val response = ShrineOrchestrator.pmPoster.post(pmRequest.toI2b2String)

      HiveConfig.fromI2b2(response.body).toXmlString
    }
    else notAnAdapter
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
    ShrineOrchestrator.broadcasterMultiplexerService.fold(notAHub) { broadcasterMultiplexerService =>

      val maxQueryWaitTime = broadcasterMultiplexerService.maxQueryWaitTime
      val broadcaster: Broadcaster = broadcasterMultiplexerService.broadcaster
      val message = newBroadcastMessageWithRunQueryRequest
      val multiplexer = broadcaster.broadcast(message)
      val responses = Await.result(multiplexer.responses, maxQueryWaitTime).toSeq
      val failures = responses.collect { case f: Failure => f }
      val timeouts = responses.collect { case t: Timeout => t }
      val validResults = responses.collect { case r: Result => r }
      val noProblems = failures.isEmpty && timeouts.isEmpty

      XmlUtil.stripWhitespace {
        <net>
          <shouldQuerySelf>
            {ShrineOrchestrator.localAdapterServiceOption.isDefined}
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
      }.toString
    }
  }

  val adapterStatusQuery = ShrineOrchestrator.shrineConfig.getString("networkStatusQuery")
  private def newRunQueryRequest(authn: AuthenticationInfo): RunQueryRequest = {
    val queryDefinition = QueryDefinition("TestQuery", OccuranceLimited(1, Term(adapterStatusQuery)))

    import scala.concurrent.duration._

    RunQueryRequest(
      "happyProject",
      3.minutes,
      authn,
      None,
      None,
      Set(ResultOutputType.PATIENT_COUNT_XML),
      queryDefinition)
  }

  private def newBroadcastMessageWithRunQueryRequest: BroadcastMessage = {
    val req = newRunQueryRequest(networkAuthn)

    ShrineOrchestrator.signerVerifier.sign(BroadcastMessage(req.networkQueryId, networkAuthn, req), SigningCertStrategy.Attach)
  }

  override def adapterReport: String = {
    val report = for {
      adapterRequestHandler <- ShrineOrchestrator.adapterService
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
          {
            resultAttempt match {
              case scala.util.Failure(cause) => failureToXml(Failure(NodeId("Local"), cause))
              case scala.util.Success(Result(origin, elapsed, response)) => {
                <result>
                  <description>{ origin }</description>
                  <elapsed> { elapsed }</elapsed>
                  <response> { response.toXml }</response>
                </result>
              }
            }
          }
        </adapter>
      }.toString
    }

    report.getOrElse(notAnAdapter)
  }

  override def auditReport: String = {

    val report = for {
      auditDao <- ShrineOrchestrator.queryEntryPointComponents.map(_.auditDao)
    } yield {
      val recentEntries = auditDao.findRecentEntries(10)

      XmlUtil.stripWhitespace {
        <recentAuditEntries>
          {
            recentEntries map { entry =>
              <entry>
                <id>{ entry.id }</id>
                <time>{ entry.time }</time>
                <username>{ entry.username }</username>
              </entry>
            }
          }
        </recentAuditEntries>
      }.toString
    }

    report.getOrElse(notAHub)
  }

  override def queryReport: String = {
    val report = for {
      adapterDao <- ShrineOrchestrator.adapterDao
    } yield {
      val recentQueries = adapterDao.findRecentQueries(10)

      XmlUtil.stripWhitespace {
        <recentQueries>
          {
            recentQueries.map { query =>
              <query>
                <id>{ query.networkId }</id>
                <date>{ query.dateCreated }</date>
                <name>{ query.name }</name>
              </query>
            }
          }
        </recentQueries>
      }.toString
    }

    report.getOrElse(notAnAdapter)
  }

  override def versionReport: String = XmlUtil.stripWhitespace {
    <versionInfo>
      <shrineVersion>{ Versions.version }</shrineVersion>
      <ontologyVersion>{ ShrineOrchestrator.ontologyMetadata.ontologyVersion }</ontologyVersion>
      <adapterMappingsVersion>{ ShrineOrchestrator.adapterMappings.map(_.version).getOrElse("No adapter mappings present") }</adapterMappingsVersion>
      <scmRevision>{ Versions.scmRevision }</scmRevision>
      <scmBranch>{ Versions.scmBranch }</scmBranch>
      <buildDate>{ Versions.buildDate }</buildDate>
    </versionInfo>
  }.toString

  override def all: String = {
    s"<all>$versionReport$keystoreReport$routingReport$hiveReport$networkReport$adapterReport$auditReport$queryReport</all>"
  }
}
