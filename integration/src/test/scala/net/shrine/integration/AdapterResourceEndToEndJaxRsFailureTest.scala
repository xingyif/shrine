package net.shrine.integration

import org.junit.Test
import net.shrine.protocol.{BroadcastMessage, DefaultBreakdownResultOutputTypes, DeleteQueryRequest, DeleteQueryResponse, ErrorResponse, Result}
import net.shrine.crypto.DefaultSignerVerifier
import net.shrine.crypto.TestKeystore

import scala.concurrent.Await
import com.sun.jersey.api.client.UniformInterfaceException
import net.shrine.adapter.service.AdapterResource
import net.shrine.crypto.SigningCertStrategy

/**
 * @author clint
 * @since Dec 17, 2013
 */
final class AdapterResourceEndToEndJaxRsFailureTest extends AbstractAdapterResourceJaxRsTest
{
  
  override val makeHandler = AlwaysThrowsAdapterRequestHandler
  
  @Test
  def testHandleRequestWithServerSideException {
    import scala.concurrent.duration._
    
    val masterId = 12345L
    
    val unsigned = BroadcastMessage(networkAuthn, DeleteQueryRequest("some-project", 1.minute, networkAuthn, masterId))
    
    val signer = new DefaultSignerVerifier(TestKeystore.certCollection)
    
    val signed = signer.sign(unsigned, SigningCertStrategy.Attach)

    val result = Await.result(client.query(signed), 1.hour)

    result.response.isInstanceOf[ErrorResponse] should be(true)
  }
  
  @Test
  def testHandleRequestWithBadInput {
    val resource = AdapterResource(MockAdapterRequestHandler)
    
    intercept[Exception] {
      resource.handleRequest("aslkdjlaksjdlasdj")
    }
  }
}