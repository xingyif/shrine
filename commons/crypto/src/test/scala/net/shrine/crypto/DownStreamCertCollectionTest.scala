package net.shrine.crypto

import java.math.BigInteger
import java.security.{KeyPairGenerator, PrivateKey, SecureRandom}
import java.util.Date

import net.shrine.util.NonEmptySeq
import org.bouncycastle.asn1.ASN1Sequence
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509._
import org.bouncycastle.cert.X509v3CertificateBuilder
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FlatSpec, Matchers}

/**
  * Created by ty on 11/1/16.
  */
@RunWith(classOf[JUnitRunner])
class DownStreamCertCollectionTest extends FlatSpec with Matchers {
  val descriptor = NewTestKeyStore.descriptor
  val heyo       = "Heyo!".getBytes("UTF-8")


  "A down stream cert collection" should "build and verify its own messages" in {
    val hubCertCollection = BouncyKeyStoreCollection.fromFileRecoverWithClassPath(descriptor) match {
      case hub: DownStreamCertCollection => hub
      case _                             => fail("This should generate a DownstreamCertCollection!")
    }

    val testEntry = CertificateCreator.createSelfSignedCertEntry("notTrusted", "testing", "stillTesting")

    hubCertCollection.allEntries.size shouldBe 2
    hubCertCollection.myEntry.privateKey.isDefined shouldBe true
    hubCertCollection.caEntry.privateKey.isDefined shouldBe false
    hubCertCollection.myEntry.aliases.first shouldBe "shrine-test"
    hubCertCollection.caEntry.aliases.first shouldBe "shrine-test-ca"
    hubCertCollection.caEntry.wasSignedBy(hubCertCollection.myEntry) shouldBe false
    hubCertCollection.myEntry.wasSignedBy(hubCertCollection.caEntry) shouldBe true

    val mySigned = hubCertCollection.myEntry.sign(heyo).get
    val testSigned = testEntry.sign(heyo).get

    testEntry.verify(mySigned, heyo) shouldBe false
    testEntry.verify(testSigned, heyo) shouldBe true
    testEntry.signed(testEntry.cert) shouldBe true

    hubCertCollection.myEntry.verify(testSigned, heyo) shouldBe false
    hubCertCollection.myEntry.verify(mySigned, heyo) shouldBe true

    hubCertCollection.caEntry.verify(testSigned, heyo) shouldBe false
    hubCertCollection.caEntry.verify(mySigned, heyo) shouldBe false

    hubCertCollection.verifyBytes(hubCertCollection.signBytes(heyo), heyo) shouldBe true
  }
}


