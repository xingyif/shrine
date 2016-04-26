package net.shrine.integration

import java.net.URL

import net.shrine.log.Loggable

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import org.junit.Test
import net.shrine.util.ShouldMatchersForJUnit
import net.shrine.adapter.AdapterMap
import net.shrine.adapter.DeleteQueryAdapter
import net.shrine.adapter.client.AdapterClient
import net.shrine.adapter.dao.squeryl.AbstractSquerylAdapterTest
import net.shrine.adapter.service.AdapterRequestHandler
import net.shrine.adapter.service.AdapterService
import net.shrine.broadcaster.AdapterClientBroadcaster
import net.shrine.broadcaster.NodeHandle
import net.shrine.crypto.DefaultSignerVerifier
import net.shrine.crypto.TestKeystore
import net.shrine.protocol.{HiveCredentials, AuthenticationInfo, BroadcastMessage, Credential, DeleteQueryRequest, DeleteQueryResponse, NodeId, Result, RunQueryRequest, CertId, RequestType, FlagQueryRequest, FlagQueryResponse, RawCrcRunQueryResponse, ResultOutputType, QueryResult, RunQueryResponse, AggregatedRunQueryResponse, UnFlagQueryRequest, UnFlagQueryResponse, DefaultBreakdownResultOutputTypes}
import net.shrine.qep.QepService
import net.shrine.broadcaster.SigningBroadcastAndAggregationService
import net.shrine.broadcaster.InJvmBroadcasterClient
import net.shrine.adapter.FlagQueryAdapter
import net.shrine.protocol.query.Term
import net.shrine.adapter.RunQueryAdapter
import net.shrine.client.Poster
import net.shrine.client.HttpClient
import net.shrine.client.HttpResponse
import net.shrine.adapter.translators.QueryDefinitionTranslator
import net.shrine.adapter.translators.ExpressionTranslator
import net.shrine.util.XmlDateHelper
import net.shrine.adapter.ReadQueryResultAdapter
import net.shrine.protocol.query.QueryDefinition
import net.shrine.adapter.UnFlagQueryAdapter
import net.shrine.crypto.SigningCertStrategy

/**
 * @author clint
 * @since Nov 27, 2013
 *
 * An in-JVM simulation of a Shrine network with one hub and 4 doanstream adapters.
 *
 * The hub and adapters are wired up with mock AdapterClients that do in-JVM communication via method calls
 * instead of remotely.
 *
 * The adapters are configured to respond with valid results for DeleteQueryRequests
 * only.  Other requests could be handled, but that would not provide benefit to offset the effort of wiring
 * up more and more-complex Adapters.
 *
 * The test network is queried, and the final result, as well as the state of each adapter, is inspected to
 * ensure that the right messages were sent between elements of the system.
 *
 */
final class NetworkSimulationTest extends AbstractSquerylAdapterTest with ShouldMatchersForJUnit {

  private val certCollection = TestKeystore.certCollection

  private lazy val myCertId: CertId = certCollection.myCertId.get

  private lazy val signerVerifier = new DefaultSignerVerifier(certCollection)

  private val domain = "test-domain"

  private val username = "test-username"

  private val password = "test-password"

  import NetworkSimulationTest._

  import scala.concurrent.duration._

  private def deleteQueryAdapter: DeleteQueryAdapter = new DeleteQueryAdapter(dao)

  private def flagQueryAdapter: FlagQueryAdapter = new FlagQueryAdapter(dao)
  
  private def unFlagQueryAdapter: UnFlagQueryAdapter = new UnFlagQueryAdapter(dao)

  private def mockPoster = Poster("http://example.com", new HttpClient {
    override def post(input: String, url: String): HttpResponse = ???
  })

  private val hiveCredentials = HiveCredentials("d", "u", "pwd", "pid")

  private def queuesQueriesRunQueryAdapter: RunQueryAdapter = {
    val translator = new QueryDefinitionTranslator(new ExpressionTranslator(Map("n1" -> Set("l1"))))

    RunQueryAdapter(
      mockPoster,
      dao,
      hiveCredentials,
      translator,
      10000,
      doObfuscation = false,
      runQueriesImmediately = false,
      DefaultBreakdownResultOutputTypes.toSet,
      collectAdapterAudit = false
    )
  }

  private def immediatelyRunsQueriesRunQueryAdapter(setSize: Long): RunQueryAdapter = {
    val mockCrcPoster = Poster("http://example.com", new HttpClient {
      override def post(input: String, url: String): HttpResponse = {
        val req = RunQueryRequest.fromI2b2String(DefaultBreakdownResultOutputTypes.toSet)(input).get

        val now = XmlDateHelper.now

        val queryResult = QueryResult(1L, 42L, Some(ResultOutputType.PATIENT_COUNT_XML), setSize, Some(now), Some(now), Some("desc"), QueryResult.StatusType.Finished, Some("status"))

        val mockCrcXml = RawCrcRunQueryResponse(req.networkQueryId, XmlDateHelper.now, req.authn.username, req.projectId, req.queryDefinition, 42L, Map(ResultOutputType.PATIENT_COUNT_XML -> Seq(queryResult))).toI2b2String

        HttpResponse.ok(mockCrcXml)
      }
    })

    queuesQueriesRunQueryAdapter.copy(poster = mockCrcPoster, runQueriesImmediately = true)
  }

  private def readQueryResultAdapter(setSize: Long): ReadQueryResultAdapter = {
    new ReadQueryResultAdapter(
      mockPoster,
      hiveCredentials,
      dao,
      doObfuscation = false,
      DefaultBreakdownResultOutputTypes.toSet,
      collectAdapterAudit = false
    )
  }

  private lazy val adaptersByNodeId: Seq[(NodeId, MockAdapterRequestHandler)] = {
    import NodeName._
    import RequestType.{ MasterDeleteRequest => MasterDeleteRequestRT, FlagQueryRequest => FlagQueryRequestRT, QueryDefinitionRequest => RunQueryRT, GetQueryResult => ReadQueryResultRT, UnFlagQueryRequest => UnFlagQueryRequestRT }

    (for {
      (childName, setSize) <- Seq((A, 1L), (B, 2L), (C, 3L), (D, 4L))
    } yield {
      val nodeId = NodeId(childName.name)
      val maxSignatureAge = 1.hour
      val adapterMap = AdapterMap(Map(
          MasterDeleteRequestRT -> deleteQueryAdapter, 
          FlagQueryRequestRT -> flagQueryAdapter, 
          UnFlagQueryRequestRT -> unFlagQueryAdapter,
          RunQueryRT -> queuesQueriesRunQueryAdapter, 
          ReadQueryResultRT -> readQueryResultAdapter(setSize)))

      nodeId -> MockAdapterRequestHandler(new AdapterService(nodeId, signerVerifier, maxSignatureAge, adapterMap))
    })
  }

  private lazy val shrineService: QepService = {
    val destinations: Set[NodeHandle] = {
      (for {
        (nodeId, adapterRequestHandler) <- adaptersByNodeId
      } yield {
        NodeHandle(nodeId, MockAdapterClient(nodeId, adapterRequestHandler))
      }).toSet
    }

    QepService(
      "example.com",
      MockAuditDao,
      MockAuthenticator,
      MockQueryAuthorizationService,
      true,
      SigningBroadcastAndAggregationService(InJvmBroadcasterClient(AdapterClientBroadcaster(destinations, MockHubDao)), signerVerifier, SigningCertStrategy.Attach),
      1.hour,
      DefaultBreakdownResultOutputTypes.toSet,
      false)
  }

  @Test
  def testSimulatedNetwork = afterCreatingTables {
    val authn = AuthenticationInfo(domain, username, Credential(password, false))

    val masterId = 12345L

    import scala.concurrent.duration._

    val req = DeleteQueryRequest("some-project-id", 1.second, authn, masterId)

    val resp = shrineService.deleteQuery(req, true)

    for {
      (nodeId, mockAdapter) <- adaptersByNodeId
    } {
      mockAdapter.lastMessage.networkAuthn.domain should equal(authn.domain)
      mockAdapter.lastMessage.networkAuthn.username should equal(authn.username)
      mockAdapter.lastMessage.request should equal(req)
      mockAdapter.lastResult.response should equal(DeleteQueryResponse(masterId))
    }

    resp should equal(DeleteQueryResponse(masterId))
  }

  @Test
  def testQueueQuery = afterCreatingTables {
    val authn = AuthenticationInfo(domain, username, Credential(password, false))

    val topicId = "askldjlkas"
    val topicName = "Topic Name"
    val queryName = "lsadj3028940"

    import scala.concurrent.duration._

    val runQueryReq = RunQueryRequest("some-project-id", 1.second, authn, 12345L, Some(topicId), Some(topicName), Set(ResultOutputType.PATIENT_COUNT_XML), QueryDefinition(queryName, Term("n1")))

    val aggregatedRunQueryResp = shrineService.runQuery(runQueryReq, true).asInstanceOf[AggregatedRunQueryResponse]

    var broadcastMessageId: Option[Long] = None

    //Broadcast the original run query request; all nodes should queue the query
    for {
      (nodeId, mockAdapter) <- adaptersByNodeId
    } {
      broadcastMessageId = Option(mockAdapter.lastMessage.requestId)

      mockAdapter.lastMessage.networkAuthn.domain should equal(authn.domain)
      mockAdapter.lastMessage.networkAuthn.username should equal(authn.username)

      val lastReq = mockAdapter.lastMessage.request.asInstanceOf[RunQueryRequest]

      lastReq.authn should equal(runQueryReq.authn)
      lastReq.requestType should equal(runQueryReq.requestType)
      lastReq.waitTime should equal(runQueryReq.waitTime)
//todo what to do with this check?      lastReq.networkQueryId should equal(mockAdapter.lastMessage.requestId)
      lastReq.outputTypes should equal(runQueryReq.outputTypes)
      lastReq.projectId should equal(runQueryReq.projectId)
      lastReq.queryDefinition should equal(runQueryReq.queryDefinition)
      lastReq.topicId should equal(runQueryReq.topicId)

      val runQueryResp = mockAdapter.lastResult.response.asInstanceOf[RunQueryResponse]

      runQueryResp.queryId should equal(-1L)
      runQueryResp.singleNodeResult.statusType should equal(QueryResult.StatusType.Held)
      runQueryResp.singleNodeResult.setSize should equal(-1L)
    }

    aggregatedRunQueryResp.queryId should equal(broadcastMessageId.get)
    aggregatedRunQueryResp.results.map(_.setSize) should equal(Seq(-1L, -1L, -1L, -1L, -4L))
  }

  @Test
  def testFlagQuery = afterCreatingTables {
    val authn = AuthenticationInfo(domain, username, Credential(password, false))

    val masterId = 12345L

    import scala.concurrent.duration._

    val networkQueryId = 9999L

    val name = "some query"
    val expr = Term("foo")
    val fooQuery = QueryDefinition(name,expr)
    dao.insertQuery(masterId.toString, networkQueryId, authn, fooQuery, isFlagged = false, hasBeenRun = true, flagMessage = None)

    dao.findQueryByNetworkId(networkQueryId).get.isFlagged should be(false)
    dao.findQueryByNetworkId(networkQueryId).get.flagMessage should be(None)

    val req = FlagQueryRequest("some-project-id", 1.second, authn, networkQueryId, Some("foo"))

    val resp = shrineService.flagQuery(req, true)

    resp should equal(FlagQueryResponse)

    dao.findQueryByNetworkId(networkQueryId).get.isFlagged should be(true)
    dao.findQueryByNetworkId(networkQueryId).get.flagMessage should be(Some("foo"))
  }
  
  @Test
  def testUnFlagQuery = afterCreatingTables {
    val authn = AuthenticationInfo(domain, username, Credential(password, false))

    val masterId = 12345L

    import scala.concurrent.duration._

    val networkQueryId = 9999L

    val flagMsg = Some("foo")

    val name = "some query"
    val expr = Term("foo")
    val fooQuery = QueryDefinition(name,expr)
    dao.insertQuery(masterId.toString, networkQueryId, authn, fooQuery, isFlagged = true, hasBeenRun = true, flagMessage = flagMsg)

    dao.findQueryByNetworkId(networkQueryId).get.isFlagged should be(true)
    dao.findQueryByNetworkId(networkQueryId).get.flagMessage should be(flagMsg)

    val req = UnFlagQueryRequest("some-project-id", 1.second, authn, networkQueryId)

    val resp = shrineService.unFlagQuery(req, true)

    resp should equal(UnFlagQueryResponse)

    dao.findQueryByNetworkId(networkQueryId).get.isFlagged should be(false)
    dao.findQueryByNetworkId(networkQueryId).get.flagMessage should be(None)
  }
}

object NetworkSimulationTest {

  private final case class MockAdapterClient(nodeId: NodeId, adapter: AdapterRequestHandler) extends AdapterClient with Loggable {
    import scala.concurrent.ExecutionContext.Implicits.global

    override def query(message: BroadcastMessage): Future[Result] = Future.successful {

      debug(s"Invoking Adapter $nodeId with $message")

      val result = adapter.handleRequest(message)

      debug(s"Got result from $nodeId: $result")

      result
    }
    override def url: Option[URL] = ???

  }

  private final case class MockAdapterRequestHandler(delegate: AdapterRequestHandler) extends AdapterRequestHandler {
    @volatile var lastMessage: BroadcastMessage = _

    @volatile var lastResult: Result = _

    override def handleRequest(request: BroadcastMessage): Result = {
      lastMessage = request

      val result = delegate.handleRequest(request)

      lastResult = result

      result
    }
  }
}

