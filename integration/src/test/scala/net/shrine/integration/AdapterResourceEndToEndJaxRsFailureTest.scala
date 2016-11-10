package net.shrine.integration

import net.shrine.adapter.service.AdapterResource
import net.shrine.crypto2.{NewTestKeyStore, SignerVerifierAdapter, SigningCertStrategy}
import net.shrine.protocol.{BroadcastMessage, DeleteQueryRequest, ErrorResponse}
import org.junit.Test

import scala.concurrent.Await

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
    
    val signer = SignerVerifierAdapter(NewTestKeyStore.certCollection)
    
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