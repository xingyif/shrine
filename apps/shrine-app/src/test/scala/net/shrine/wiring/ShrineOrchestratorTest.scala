package net.shrine.wiring

import java.net.URL
import javax.ws.rs.core.MediaType

import net.shrine.client.{EndpointConfig, JerseyHttpClient}
import net.shrine.crypto.{NewTestKeyStore, TrustParam}
import net.shrine.util.ShouldMatchersForJUnit
import org.junit.Test

/**
 * @author clint
 * @since Jan 7, 2014
 */
final class ShrineOrchestratorTest extends ShouldMatchersForJUnit {

  @Test
  def testMakeHttpClient {

    val url = new URL("http://example.com")
    
    import scala.concurrent.duration._
      
    //AcceptAllCerts
    {
      val endpoint = EndpointConfig(url, true, 42.minutes)
      
      val JerseyHttpClient(trustParam, timeout, mediaType, credentials) = JerseyHttpClient(NewTestKeyStore.certCollection, endpoint)
      
      trustParam should be(TrustParam.AcceptAllCerts)
      timeout should be(endpoint.timeout)
      mediaType should be(MediaType.TEXT_XML)
      credentials should be(None)
    }
    
    //Don't accept all certs
    {
      val endpoint = EndpointConfig(url, false, 42.minutes)
      
      val JerseyHttpClient(trustParam, timeout, mediaType, credentials) = JerseyHttpClient(NewTestKeyStore.certCollection, endpoint)
      
      trustParam should be(TrustParam.BouncyKeyStore(NewTestKeyStore.certCollection))
      timeout should be(endpoint.timeout)
      mediaType should be(MediaType.TEXT_XML)
      credentials should be(None)
    }
  }
}