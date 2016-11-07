package net.shrine.integration

import scala.concurrent.Await
import org.junit.Test
import net.shrine.adapter.client.AdapterClient
import net.shrine.adapter.client.RemoteAdapterClient
import net.shrine.client.JerseyHttpClient
import net.shrine.client.Poster
import net.shrine.client.TimeoutException
import net.shrine.crypto.DefaultSignerVerifier
import net.shrine.crypto.NewTestKeyStore
import net.shrine.crypto.TrustParam.AcceptAllCerts
import net.shrine.protocol.{BroadcastMessage, DefaultBreakdownResultOutputTypes, DeleteQueryRequest, NodeId}
import net.shrine.crypto.SigningCertStrategy
import net.shrine.crypto2.SignerVerifierAdapter

/**
 * @author clint
 * @date Dec 17, 2013
 */
final class AdapterResourceEndToEndJaxRsTimeoutTest extends AbstractAdapterResourceJaxRsTest {
  
  import scala.concurrent.duration._
  
  override def makeHandler = TimesOutAdapterRequestHandler(1.minute)
  
  override protected lazy val client: AdapterClient = {
    RemoteAdapterClient(NodeId.Unknown,Poster(resourceUrl, JerseyHttpClient(AcceptAllCerts, 100.milliseconds)), DefaultBreakdownResultOutputTypes.toSet)
  }
  
  @Test
  def testHandleRequestTimeout {
    val masterId = 12345L
    
    val unsigned = BroadcastMessage(networkAuthn, DeleteQueryRequest("some-project", 1.minute, networkAuthn, masterId))
    
    val signer = SignerVerifierAdapter(NewTestKeyStore.certCollection)
    
    val signed = signer.sign(unsigned, SigningCertStrategy.Attach)
    
    //Client timeouts should result in a net.shrine.client.TimeoutException being thrown 
    intercept[TimeoutException] {
      Await.result(client.query(signed), 1.minute)
    }
  }
}