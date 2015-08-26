package net.shrine.integration

import scala.concurrent.duration.DurationInt
import net.shrine.adapter.client.RemoteAdapterClient
import net.shrine.adapter.service.AdapterRequestHandler
import net.shrine.adapter.service.AdapterResource
import net.shrine.adapter.service.JerseyTestComponent
import net.shrine.broadcaster.AdapterClientBroadcaster
import net.shrine.broadcaster.InJvmBroadcasterClient
import net.shrine.broadcaster.NodeHandle
import net.shrine.client.JerseyHttpClient
import net.shrine.client.Poster
import net.shrine.crypto.DefaultSignerVerifier
import net.shrine.crypto.TestKeystore
import net.shrine.crypto.TrustParam
import net.shrine.protocol.AuthenticationInfo
import net.shrine.protocol.CertId
import net.shrine.protocol.Credential
import net.shrine.protocol.NodeId
import net.shrine.protocol.ShrineRequestHandler
import net.shrine.service.ShrineResource
import net.shrine.service.ShrineService
import org.junit.Before
import org.junit.After
import net.shrine.protocol.DefaultBreakdownResultOutputTypes


/**
 * @author clint
 * @date Mar 6, 2014
 */
trait AbstractHubAndSpokesTest {
  
  @Before
  def setUp(): Unit = {
    spokes.foreach(_.JerseyTest.setUp())
  }

  @After
  def tearDown(): Unit = {
    spokes.foreach(_.JerseyTest.tearDown())
  }
  
  def posterFor(component: JerseyTestComponent[_]): Poster = Poster(component.resourceUrl, JerseyHttpClient(TrustParam.AcceptAllCerts, 30.minutes))
  
  import scala.concurrent.duration._
  
  val networkAuthn = AuthenticationInfo("d", "u", Credential("p", false))
  
  val certCollection = TestKeystore.certCollection

  lazy val myCertId: CertId = certCollection.myCertId.get

  lazy val signerVerifier = new DefaultSignerVerifier(certCollection)
  
  import AbstractHubAndSpokesTest.SpokeComponent
  
  lazy val Spoke0Component = SpokeComponent(9998, NodeId("Spoke 0"))

  lazy val Spoke1Component = SpokeComponent(9999, NodeId("Spoke 1"))
  
  lazy val spokes: Set[SpokeComponent] = Set(Spoke0Component, Spoke1Component)
}

object AbstractHubAndSpokesTest {
  final case class SpokeComponent(override val port: Int, nodeId: NodeId) extends JerseyTestComponent[AdapterRequestHandler] {
    override val basePath = "adapter"

    def mockHandler = handler.asInstanceOf[MockAdapterRequestHandler]
      
    override lazy val makeHandler: AdapterRequestHandler = new MockAdapterRequestHandler(nodeId)
    
    override def resourceClass(handler: AdapterRequestHandler) = AdapterResource(handler)
  }
}