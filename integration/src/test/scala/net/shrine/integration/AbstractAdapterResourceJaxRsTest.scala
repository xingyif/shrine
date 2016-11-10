package net.shrine.integration

import scala.concurrent.duration.DurationInt
import org.junit.After
import org.junit.Before
import net.shrine.util.ShouldMatchersForJUnit
import net.shrine.adapter.client.RemoteAdapterClient
import net.shrine.adapter.service.AdapterRequestHandler
import net.shrine.adapter.service.AdapterResource
import net.shrine.adapter.service.JerseyTestComponent
import net.shrine.client.JerseyHttpClient
import net.shrine.client.Poster
import net.shrine.crypto2.TrustParam.AcceptAllCerts
import net.shrine.protocol.{NodeId, AuthenticationInfo, Credential, DefaultBreakdownResultOutputTypes}
import net.shrine.adapter.client.AdapterClient

/**
 * @author clint
 * @date Dec 17, 2013
 */
abstract class AbstractAdapterResourceJaxRsTest extends JerseyTestComponent[AdapterRequestHandler] with ShouldMatchersForJUnit {
  
  protected val networkAuthn = AuthenticationInfo("d", "u", Credential("p", false))
  
  import scala.concurrent.duration._
  
  protected val breakdownTypes = DefaultBreakdownResultOutputTypes.toSet 
  
  protected lazy val client: AdapterClient = RemoteAdapterClient(NodeId.Unknown,Poster(resourceUrl, new JerseyHttpClient(AcceptAllCerts, 1.minute)), breakdownTypes)
  
  override val basePath = "adapter"

  override def resourceClass(handler: AdapterRequestHandler) = AdapterResource(handler)
    
  @Before
  def setUp(): Unit = this.JerseyTest.setUp()

  @After
  def tearDown(): Unit = this.JerseyTest.tearDown()
}
