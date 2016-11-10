package net.shrine.crypto2

import net.shrine.protocol.CertData
import net.shrine.util.ShouldMatchersForJUnit
import org.junit.Test

/**
 * @author clint
 * @date Dec 5, 2014
 */
final class CertDataEncodingTest extends ShouldMatchersForJUnit {
  @Test
  def testToCertificateKey: Unit = {
    import net.shrine.crypto2.NewTestKeyStore.certCollection
    
    val cert = certCollection.myEntry.cert
    
    val certData = CertData(cert)
    
    certData.toCertificate should equal(cert)
  }
}