package net.shrine.integration

import net.shrine.adapter.client.RemoteAdapterClient
import net.shrine.adapter.service.JerseyTestComponent
import net.shrine.broadcaster.{AdapterClientBroadcaster, NodeHandle, PosterBroadcasterClient}
import net.shrine.broadcaster.service.{BroadcasterMultiplexerRequestHandler, BroadcasterMultiplexerResource, BroadcasterMultiplexerService}
import net.shrine.protocol.query.{Constrained, Modifiers, Or, QueryDefinition, Term, ValueConstraint}
import net.shrine.protocol.{DefaultBreakdownResultOutputTypes, DeleteQueryRequest, DeleteQueryResponse, FlagQueryRequest, FlagQueryResponse, NodeId, RequestType, Result, ResultOutputType, RunQueryRequest, RunQueryResponse, UnFlagQueryRequest, UnFlagQueryResponse}
import net.shrine.util.ShouldMatchersForJUnit
import org.junit.{After, Before, Test}

/**
 * @author clint
 * @since Mar 6, 2014
 */
final class OneQepOneHubTwoSpokesJaxrsTest extends AbstractHubAndSpokesTest with ShouldMatchersForJUnit { thisTest =>
/*
  @Test
  def testBroadcastDeleteQueryShrine(): Unit = doTestBroadcastDeleteQuery(shrineQueryEntryPointComponent)
  
  @Test
  def testBroadcastDeleteQueryI2b2(): Unit = doTestBroadcastDeleteQuery(i2b2QueryEntryPointComponent)
  
  @Test
  def testBroadcastFlagQueryShrine(): Unit = doTestBroadcastFlagQuery(shrineQueryEntryPointComponent)
  
  @Test
  def testBroadcastFlagQueryI2b2(): Unit = doTestBroadcastFlagQuery(i2b2QueryEntryPointComponent)
  
  @Test
  def testBroadcastUnFlagQueryShrine(): Unit = doTestBroadcastUnFlagQuery(shrineQueryEntryPointComponent)
  
  @Test
  def testBroadcastUnFlagQueryI2b2(): Unit = doTestBroadcastUnFlagQuery(i2b2QueryEntryPointComponent)
  
  @Test
  def testBroadcastRunQueryShrine(): Unit = doTestBroadcastRunQuery(shrineQueryEntryPointComponent)
  
  @Test
  def testBroadcastRunQueryI2b2(): Unit = doTestBroadcastRunQuery(i2b2QueryEntryPointComponent)
  
  private def doTestBroadcastDeleteQuery[H <: AnyRef](queryEntryPointComponent: AbstractHubComponent[H]): Unit = {
    val masterId = 123456L
    
    val projectId = "some-project-id"
    
    val client = queryEntryPointComponent.clientFor(projectId, networkAuthn)
    
    //Broadcast a message
    val resp = client.deleteQuery(masterId, true)
    
    //Make sure we got the right response
    resp.queryId should equal(masterId)
    
    //Make sure all the spokes received the right message
    spokes.foreach { spoke =>
      val lastMessage = spoke.mockHandler.lastMessage.get
      
      lastMessage.networkAuthn.domain should equal(networkAuthn.domain)
      lastMessage.networkAuthn.username should equal(networkAuthn.username)
      
      val req = lastMessage.request.asInstanceOf[DeleteQueryRequest]
      
      req.networkQueryId should equal(masterId)
      req.projectId should equal(projectId)
      req.requestType should equal(RequestType.MasterDeleteRequest)
      req.authn should equal(networkAuthn)
    }
    
    //Make sure we got the right responses at the hub
    
    val multiplexer = HubComponent.broadcaster.lastMultiplexer.get
    
    val expectedResponses = spokes.map { spoke =>
      Result(spoke.nodeId, spoke.mockHandler.elapsed, DeleteQueryResponse(masterId))
    }.toSet
    
    multiplexer.resultsSoFar.toSet should equal(expectedResponses)
  }
  
  private def doTestBroadcastRunQuery[H <: AnyRef](queryEntryPointComponent: AbstractHubComponent[H]): Unit = {
    val masterId = 123456L
    
    val projectId = "some-project-id"
    
    val client = queryEntryPointComponent.clientFor(projectId, networkAuthn)
    
    //Include a modified term, to ensure they're parsed properly
    val queryDefinition = QueryDefinition("foo", Or(Term("x"), Constrained(Term("y"), Modifiers("some-modifier", "ap", "k"), ValueConstraint("foo", Some("bar"), "baz", "nuh"))))
    
    //Broadcast a message
    val resp = client.runQuery("some-topic-id", Set(ResultOutputType.PATIENT_COUNT_XML), queryDefinition, true)
    
    resp.results.size should equal(spokes.size)
    
    //Make sure all the spokes received the right message
    spokes.foreach { spoke =>
      val lastMessage = spoke.mockHandler.lastMessage.get
      
      lastMessage.networkAuthn.domain should equal(networkAuthn.domain)
      lastMessage.networkAuthn.username should equal(networkAuthn.username)
      
      val req = lastMessage.request.asInstanceOf[RunQueryRequest]
      
      req.projectId should equal(projectId)
      req.requestType should equal(RequestType.QueryDefinitionRequest)
      req.authn should equal(networkAuthn)
      req.queryDefinition should equal(queryDefinition)
    }
    
    //Make sure we got the right responses at the hub
    
    val multiplexer = HubComponent.broadcaster.lastMultiplexer.get
    
    multiplexer.resultsSoFar.collect { case Result(_, _, payload) => payload.getClass } should equal((1 to spokes.size).map(_ => classOf[RunQueryResponse]))
    
    val expectedResponders = spokes.map(_.nodeId).toSet
    
    multiplexer.resultsSoFar.map(_.origin).toSet should equal(expectedResponders)
  }
  
  private def doTestBroadcastFlagQuery[H <: AnyRef](queryEntryPointComponent: AbstractHubComponent[H]): Unit = {
    val networkQueryId = 123456L
    
    val projectId = "some-project-id"
    
    val client = queryEntryPointComponent.clientFor(projectId, networkAuthn)
    
    val message = "flag message"
    
    //Broadcast a message
    val resp = client.flagQuery(networkQueryId, Some(message), true)
    
    //Make sure we got the right response
    resp should be(FlagQueryResponse)
    
    //Make sure all the spokes received the right message
    spokes.foreach { spoke =>
      val lastMessage = spoke.mockHandler.lastMessage.get
      
      lastMessage.networkAuthn.domain should equal(networkAuthn.domain)
      lastMessage.networkAuthn.username should equal(networkAuthn.username)
      
      val req = lastMessage.request.asInstanceOf[FlagQueryRequest]
      
      req.networkQueryId should equal(networkQueryId)
      req.projectId should equal(projectId)
      req.requestType should equal(RequestType.FlagQueryRequest)
      req.authn should equal(networkAuthn)
      req.message should be(Some(message))
    }
    
    //Make sure we got the right responses at the hub
    
    val multiplexer = HubComponent.broadcaster.lastMultiplexer.get
    
    val expectedResponses = spokes.map { spoke =>
      Result(spoke.nodeId, spoke.mockHandler.elapsed, FlagQueryResponse)
    }.toSet
    
    multiplexer.resultsSoFar.toSet should equal(expectedResponses)
  }
  
  private def doTestBroadcastUnFlagQuery[H <: AnyRef](queryEntryPointComponent: AbstractHubComponent[H]): Unit = {
    val networkQueryId = 123456L
    
    val projectId = "some-project-id"
    
    val client = queryEntryPointComponent.clientFor(projectId, networkAuthn)
    
    //Broadcast a message
    val resp = client.unFlagQuery(networkQueryId, true)
    
    //Make sure we got the right response
    resp should be(UnFlagQueryResponse)
    
    //Make sure all the spokes received the right message
    spokes.foreach { spoke =>
      val lastMessage = spoke.mockHandler.lastMessage.get
      
      lastMessage.networkAuthn.domain should equal(networkAuthn.domain)
      lastMessage.networkAuthn.username should equal(networkAuthn.username)
      
      val req = lastMessage.request.asInstanceOf[UnFlagQueryRequest]
      
      req.networkQueryId should equal(networkQueryId)
      req.projectId should equal(projectId)
      req.requestType should equal(RequestType.UnFlagQueryRequest)
      req.authn should equal(networkAuthn)
    }
    
    //Make sure we got the right responses at the hub
    
    val multiplexer = HubComponent.broadcaster.lastMultiplexer.get
    
    val expectedResponses = spokes.map { spoke =>
      Result(spoke.nodeId, spoke.mockHandler.elapsed, UnFlagQueryResponse)
    }.toSet
    
    multiplexer.resultsSoFar.toSet should equal(expectedResponses)
  }
  
  import scala.concurrent.duration._
  
  lazy val i2b2QueryEntryPointComponent = Hubs.I2b2(thisTest, port = 9995, broadcasterClient = Some(PosterBroadcasterClient(posterFor(HubComponent), DefaultBreakdownResultOutputTypes.toSet)))
  
  lazy val shrineQueryEntryPointComponent = Hubs.Shrine(thisTest, port = 9996, broadcasterClient = Some(PosterBroadcasterClient(posterFor(HubComponent), DefaultBreakdownResultOutputTypes.toSet)))
  
  object HubComponent extends JerseyTestComponent[BroadcasterMultiplexerRequestHandler] {
    override val basePath = "broadcaster/broadcast"

    override val port = 9997
      
    override def resourceClass(handler: BroadcasterMultiplexerRequestHandler) = BroadcasterMultiplexerResource(handler)
    
    lazy val broadcaster: InspectableDelegatingBroadcaster = {
      val destinations: Set[NodeHandle] = spokes.map { spoke =>
        val client = RemoteAdapterClient(NodeId.Unknown,posterFor(spoke), DefaultBreakdownResultOutputTypes.toSet)
        
        NodeHandle(spoke.nodeId, client)
      }
      
      InspectableDelegatingBroadcaster(AdapterClientBroadcaster(destinations, MockHubDao))
    }
    
    override lazy val makeHandler: BroadcasterMultiplexerRequestHandler = {
      BroadcasterMultiplexerService(broadcaster, 1.hour)
    }
  }
  
  @Before
  override def setUp() {
    super.setUp()
    
    HubComponent.JerseyTest.setUp()
    shrineQueryEntryPointComponent.JerseyTest.setUp()
    i2b2QueryEntryPointComponent.JerseyTest.setUp()
  }

  @After
  override def tearDown() {
    i2b2QueryEntryPointComponent.JerseyTest.tearDown()
    shrineQueryEntryPointComponent.JerseyTest.tearDown()
    HubComponent.JerseyTest.tearDown()
    
    super.tearDown()
  }
  */
}