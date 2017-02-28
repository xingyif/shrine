package net.shrine.crypto

import org.junit.runner.RunWith
import org.scalatest.{FlatSpec, Matchers}
import org.scalatest.junit.JUnitRunner

/**
  * @by ty
  * @since 2/28/17
  */
@RunWith(classOf[JUnitRunner])
class KeyStoreEntryTest extends FlatSpec with Matchers {
  val testEntry = CertificateCreator.createSelfSignedCertEntry("notTrusted", "testing", "stillTesting")
  val test2 = CertificateCreator.createSignedCertFromEntry("notTrusted2", "testing2", "stillTesting2", testEntry)
  val test3 = CertificateCreator.createSignedCertFromEntry("notTrusted3", "testing3", "stillTesting3", test2)

  "The keystore entries" should "return that they were signed correctly" in {
    testEntry.wasSignedBy(testEntry) shouldBe true
    test2.wasSignedBy(testEntry) shouldBe true
    test3.wasSignedBy(test2) shouldBe true
  }
}
