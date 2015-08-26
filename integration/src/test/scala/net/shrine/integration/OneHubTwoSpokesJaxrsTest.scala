package net.shrine.integration

import org.junit.After
import org.junit.Before
import org.junit.Test
import net.shrine.util.ShouldMatchersForJUnit
import net.shrine.adapter.client.RemoteAdapterClient
import net.shrine.adapter.service.AdapterRequestHandler
import net.shrine.adapter.service.AdapterResource
import net.shrine.adapter.service.JerseyTestComponent
import net.shrine.broadcaster.AdapterClientBroadcaster
import net.shrine.broadcaster.NodeHandle
import net.shrine.client.JerseyHttpClient
import net.shrine.client.JerseyShrineClient
import net.shrine.client.Poster
import net.shrine.crypto.DefaultSignerVerifier
import net.shrine.crypto.TestKeystore
import net.shrine.crypto.TrustParam
import net.shrine.protocol.AuthenticationInfo
import net.shrine.protocol.Credential
import net.shrine.protocol.DeleteQueryRequest
import net.shrine.protocol.DeleteQueryResponse
import net.shrine.protocol.NodeId
import net.shrine.protocol.RequestType
import net.shrine.protocol.Result
import net.shrine.protocol.ShrineRequestHandler
import net.shrine.service.ShrineResource
import net.shrine.service.ShrineService
import net.shrine.protocol.CertId
import net.shrine.broadcaster.InJvmBroadcasterClient

/**
 * @author clint
 * @date Jan 8, 2014
 *
 * An end-to-end JAX-RS test that fires up a Hub and two spokes, makes a query,
 * and verifies that the correct requests were broadcast to the spokes, and
 * that the correct responses were received and aggregated at the hub.
 *
 * DeleteQueryResponses are used because they have very few fields and are easy
 * to construct and verify.  It might be nice in the future to use
 * RunQuery{Request,Response}, but that was more trouble than it was worth for
 * a first pass.
 *
 * NB: The hub runs on port 9997, and the two spokes run on ports 9998 and 9999.
 */
final class OneHubTwoSpokesJaxrsTest extends AbstractHubAndSpokesTest with ShouldMatchersForJUnit { thisTest =>

  import scala.concurrent.duration._

  @Test
  def testBroadcastDeleteQueryShrine: Unit = {
    doTestBroadcastDeleteQuery(shrineHubComponent)
  }
  
  @Test
  def testBroadcastDeleteQueryI2b2: Unit = {
    doTestBroadcastDeleteQuery(i2b2HubComponent)
  }
  
  lazy val shrineHubComponent = Hubs.Shrine(thisTest, port = 9997)
  
  lazy val i2b2HubComponent = Hubs.I2b2(thisTest, port = 9996)
  
  private def doTestBroadcastDeleteQuery[H <: AnyRef](hubComponent: AbstractHubComponent[H]): Unit = {
    val masterId = 123456L

    val projectId = "some-project-id"

    val client = hubComponent.clientFor(projectId, networkAuthn)

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

      req.queryId should equal(masterId)
      req.projectId should equal(projectId)
      req.requestType should equal(RequestType.MasterDeleteRequest)
      req.authn should equal(networkAuthn)
    }

    //Make sure we got the right responses at the hub

    val multiplexer = hubComponent.broadcaster.lastMultiplexer.get

    val expectedResponses = spokes.map { spoke =>
      Result(spoke.nodeId, spoke.mockHandler.elapsed, DeleteQueryResponse(masterId))
    }.toSet

    multiplexer.resultsSoFar.toSet should equal(expectedResponses)
  }

  @Before
  override def setUp() {
    super.setUp()
    shrineHubComponent.JerseyTest.setUp()
    i2b2HubComponent.JerseyTest.setUp()
  }

  @After
  override def tearDown() {
    shrineHubComponent.JerseyTest.tearDown()
    i2b2HubComponent.JerseyTest.tearDown()
    super.tearDown()
  }
}