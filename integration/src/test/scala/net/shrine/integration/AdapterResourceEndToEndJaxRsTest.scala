package net.shrine.integration

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import org.junit.Test
import net.shrine.protocol.BroadcastMessage
import net.shrine.protocol.DeleteQueryRequest
import net.shrine.protocol.DeleteQueryResponse
import net.shrine.protocol.Result
import com.sun.jersey.api.client.UniformInterfaceException
import net.shrine.crypto.{NewTestKeyStore, SignerVerifierAdapter, SigningCertStrategy}

/**
 * @author clint
 * @date Dec 17, 2013
 */
final class AdapterResourceEndToEndJaxRsTest extends AbstractAdapterResourceJaxRsTest {
  
  override val makeHandler = MockAdapterRequestHandler
  
  @Test
  def testHandleRequest {
    import scala.concurrent.duration._
    
    val masterId = 12345L
    
    val unsigned = BroadcastMessage(networkAuthn, DeleteQueryRequest("some-project", 1.minute, networkAuthn, masterId))
    
    val signer = SignerVerifierAdapter(NewTestKeyStore.certCollection)
    
    val signed = signer.sign(unsigned, SigningCertStrategy.Attach)
    
    {
      val resp = Await.result(client.query(signed), 1.hour)
      
      resp should equal(Result(MockAdapterRequestHandler.nodeId, MockAdapterRequestHandler.elapsed, DeleteQueryResponse(masterId)))
    }
  }
}