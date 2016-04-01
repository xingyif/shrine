package net.shrine.wiring

import net.shrine.util.ShouldMatchersForJUnit
import org.junit.Test
import net.shrine.adapter.service.AdapterRequestHandler
import net.shrine.protocol.BroadcastMessage
import net.shrine.protocol.Result
import net.shrine.client.{EndpointConfig, JerseyHttpClient}
import net.shrine.crypto.TestKeystore
import java.net.URL
import net.shrine.crypto.TrustParam
import javax.ws.rs.core.MediaType

/**
 * @author clint
 * @since Jan 7, 2014
 */
final class ShrineOrchestratorTest extends ShouldMatchersForJUnit {
  @Test
  def testMakeAdapterServiceOption {
    import ShrineOrchestrator.makeAdapterServiceOption
    
    makeAdapterServiceOption(false, null) should be(None)
    
    val mockAdapter: Option[AdapterRequestHandler] = Option(new AdapterRequestHandler {
      override def handleRequest(request: BroadcastMessage): Result = ???
    })
    
    makeAdapterServiceOption(true, mockAdapter) should equal(mockAdapter)
  }
  
  @Test
  def testMakeHttpClient {
    import ShrineOrchestrator.makeHttpClient
    
    val url = new URL("http://example.com")
    
    import scala.concurrent.duration._
      
    //AcceptAllCerts
    {
      val endpoint = EndpointConfig(url, true, 42.minutes)
      
      val JerseyHttpClient(trustParam, timeout, mediaType, credentials) = makeHttpClient(TestKeystore.certCollection, endpoint)
      
      trustParam should be(TrustParam.AcceptAllCerts)
      timeout should be(endpoint.timeout)
      mediaType should be(MediaType.TEXT_XML)
      credentials should be(None)
    }
    
    //Don't accept all certs
    {
      val endpoint = EndpointConfig(url, false, 42.minutes)
      
      val JerseyHttpClient(trustParam, timeout, mediaType, credentials) = makeHttpClient(TestKeystore.certCollection, endpoint)
      
      trustParam should be(TrustParam.SomeKeyStore(TestKeystore.certCollection))
      timeout should be(endpoint.timeout)
      mediaType should be(MediaType.TEXT_XML)
      credentials should be(None)
    }
  }
}