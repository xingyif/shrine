package net.shrine.crypto

import net.shrine.util.ShouldMatchersForJUnit
import org.junit.Test

/**
 * @author clint
 * @date Jan 15, 2015
 */
final class CertCollectionTest extends ShouldMatchersForJUnit {
  @Test
  def testGetIssuer: Unit = {
    val cert = NewTestKeyStore.certCollection.myEntry.cert
    
    CertCollection.getIssuer(cert) should equal(cert.getIssuerX500Principal)
    CertCollection.getIssuer(cert) should not equal(cert.getIssuerDN)
  }
}