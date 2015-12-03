package net.shrine.crypto

import net.shrine.util.ShouldMatchersForJUnit
import org.junit.Test

/**
 * @author clint
 * @date Dec 18, 2013
 */
final class TrustParamTest extends ShouldMatchersForJUnit {
  @Test
  def testForKeyStore {
    TrustParam.forKeyStore(TestKeystore.certCollection) should equal(TrustParam.SomeKeyStore(TestKeystore.certCollection))
  }
}