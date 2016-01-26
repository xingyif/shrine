package net.shrine.integration

import net.shrine.adapter.service.{AdapterRequestHandler, AdapterResource, JerseyTestComponent}
import net.shrine.client.{JerseyHttpClient, Poster}
import net.shrine.crypto.{DefaultSignerVerifier, TestKeystore, TrustParam}
import net.shrine.protocol.{AuthenticationInfo, CertId, Credential, NodeId}
import org.junit.{After, Before}

import scala.concurrent.duration.DurationInt


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