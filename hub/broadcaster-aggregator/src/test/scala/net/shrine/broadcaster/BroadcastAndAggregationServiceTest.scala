package net.shrine.broadcaster

import net.shrine.util.ShouldMatchersForJUnit
import org.junit.Test
import net.shrine.protocol.DeleteQueryRequest
import net.shrine.protocol.BroadcastMessage
import net.shrine.protocol.ShrineResponse
import net.shrine.aggregation.Aggregator
import scala.concurrent.Future
import net.shrine.protocol.RunQueryRequest
import net.shrine.protocol.AuthenticationInfo
import net.shrine.protocol.Credential
import net.shrine.protocol.query.QueryDefinition
import net.shrine.protocol.query.Term
import net.shrine.aggregation.DeleteQueryAggregator
import net.shrine.aggregation.RunQueryAggregator
import net.shrine.aggregation.ReadQueryResultAggregator

/**
 * @author clint
 * @since Mar 14, 2013
 */
final class BroadcastAndAggregationServiceTest extends ShouldMatchersForJUnit {
  import BroadcastAndAggregationServiceTest._
  
  private val authn = AuthenticationInfo("some-domain", "some-user", Credential("some-password", isToken = false))
  
  private val queryDef = QueryDefinition("yo", Term("foo"))
  
  import scala.concurrent.duration._
  
  @Test
  def testSendAndAggregateShrineRequest() {
    val service = new TestBroadcastAndAggregationService
    
    {
      val req = DeleteQueryRequest("projectId", 1.millisecond, authn, 123L)
      
      val aggregator = new DeleteQueryAggregator
      
      val networkAuthn = AuthenticationInfo("d", "u", Credential("p", isToken = false))
      
      service.sendAndAggregate(networkAuthn, req, aggregator, shouldBroadcast = true)
      
      service.args.shouldBroadcast should be(Some(true))
      
      service.sendAndAggregate(networkAuthn, req, aggregator, shouldBroadcast = false)
      
      service.args.shouldBroadcast should be(Some(false))
      service.args.aggregator should be(aggregator)
      service.args.message.networkAuthn should be(networkAuthn)
      service.args.message.request should be(req)
      (service.args.message.requestId > 0) should be(right = true)
    }
    
    {
      val invalidQueryId = -1L
      
      val req = RunQueryRequest("projectId", 1.millisecond, authn, invalidQueryId, Some("topicId"), Some("Topic Name"), Set.empty, queryDef)
      
      val aggregator = new RunQueryAggregator(invalidQueryId, authn.username, authn.domain, queryDef, true)
      
      val networkAuthn = AuthenticationInfo("d", "u", Credential("p", isToken = false))
      
      service.sendAndAggregate(networkAuthn, req, aggregator, shouldBroadcast = true)
      
      service.args.shouldBroadcast should be(Some(true))
      
      (service.args.message.requestId > 0) should be(right = true)
      service.args.message.request should not be req
      service.args.message.request.asInstanceOf[RunQueryRequest].networkQueryId should be(service.args.message.requestId)
      service.args.message.networkAuthn should be(networkAuthn)
      service.args.aggregator should not be aggregator
      service.args.aggregator.asInstanceOf[RunQueryAggregator].queryId should be(service.args.message.requestId)
    }
  }
  
  @Test
  def testAddQueryIdAggregator() {
    val service = new TestBroadcastAndAggregationService
    
    {
      val aggregator = new DeleteQueryAggregator
      
      val munged = service.addQueryId(null, aggregator)
      
      (munged eq aggregator) should be(right = true)
    }
    
    {
      val aggregator = new RunQueryAggregator(-1L, authn.username, authn.domain, queryDef, true)
      
      val message = BroadcastMessage(999L, authn, null)
      
      val munged = service.addQueryId(message, aggregator).asInstanceOf[RunQueryAggregator]
      
      munged.queryId should be(message.requestId)
      
      munged.userId should equal(aggregator.userId)
      munged.groupId should equal(aggregator.groupId)
      munged.requestQueryDefinition should equal(aggregator.requestQueryDefinition)
      munged.addAggregatedResult should equal(aggregator.addAggregatedResult)
    }
    
    def doTestWithReadQueryResultAggregator(showAggregation: Boolean) {
      val aggregator = new ReadQueryResultAggregator(-1L, showAggregation)
      
      val message = BroadcastMessage(999L, authn, null)
      
      val munged = service.addQueryId(message, aggregator).asInstanceOf[ReadQueryResultAggregator]
      
      munged should not be aggregator
      
      munged.shrineNetworkQueryId should be(message.requestId)
      
      munged.showAggregation should be(aggregator.showAggregation)
    }
    
    doTestWithReadQueryResultAggregator(true)
    doTestWithReadQueryResultAggregator(false)
  }
  
  @Test
  def testAddQueryIdShrineRequest() {
    val service = new TestBroadcastAndAggregationService
    
    {
      val req = DeleteQueryRequest("projectId", 1.millisecond, authn, 123L)
      
      val (queryIdOption, transformedReq) = service.addQueryId(req)
      
      queryIdOption should be(None)
      
      transformedReq should be(req)
    }
    
    {
      val req = RunQueryRequest("projectId", 1.millisecond, authn, -1L, Some("topicId"), Some("Topic Name"), Set.empty, QueryDefinition("yo", Term("foo")))
      
      val (queryIdOption, transformedReq: RunQueryRequest) = service.addQueryId(req)
      
      queryIdOption should not be None
      
      (queryIdOption.get > 0) should be(right = true)
      
      transformedReq.networkQueryId should be(queryIdOption.get)
      
      transformedReq.projectId should be(req.projectId)
      transformedReq.waitTime should be(req.waitTime)
      transformedReq.authn should be(req.authn)
      transformedReq.topicId should be(req.topicId)
      transformedReq.outputTypes should be(req.outputTypes)
      transformedReq.queryDefinition should be(req.queryDefinition)
    }
  }
}

object BroadcastAndAggregationServiceTest {
  private final class TestBroadcastAndAggregationService extends BroadcastAndAggregationService {
    object args {
      var message: BroadcastMessage = _
      var aggregator: Aggregator = _
      var shouldBroadcast: Option[Boolean] = None
    }
    
    override def sendAndAggregate(message: BroadcastMessage, aggregator: Aggregator, shouldBroadcast: Boolean): Future[ShrineResponse] = {
      args.message = message
      args.aggregator = aggregator
      args.shouldBroadcast = Some(shouldBroadcast)
      
      Future.successful(null)
    }

    override def attachSigningCert: Boolean = false
    override val broadcasterUrl = None
  }
}