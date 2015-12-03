package net.shrine.crypto

import net.shrine.util.ShouldMatchersForJUnit
import org.junit.Test

/**
 * @author clint
 * @date Dec 2, 2013
 */
final class KeyStoreTypeTest extends ShouldMatchersForJUnit {
  @Test
  def testDefault {
    KeyStoreType.Default should equal(KeyStoreType.PKCS12)
  }

  @Test
  def testNames {
    KeyStoreType.PKCS12.name should equal("PKCS12")

    KeyStoreType.JKS.name should equal("JKS")
  }
}