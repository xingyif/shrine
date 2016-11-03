package net.shrine.crypto2

import junit.framework.TestFailure
import net.shrine.crypto.{KeyStoreDescriptor, KeyStoreType, NewTestKeyStore}
import net.shrine.util.SingleHubModel
import org.junit.runner.RunWith
import org.scalatest.{FlatSpec, Matchers, ShouldMatchers}
import org.scalatest.junit.JUnitRunner

/**
  * Created by ty on 11/1/16.
  */
@RunWith(classOf[JUnitRunner])
class HubCertCollectionTest extends FlatSpec with Matchers {
  val descriptor = NewTestKeyStore.descriptor
  val heyo = "Heyo!".getBytes("UTF-8")


  "A hub cert collection" should "build and verify its own messages" in {
    val hubCertCollection = BouncyKeyStoreCollection.fromFileRecoverWithClassPath(descriptor) match {
      case hub:HubCertCollection => hub
      case _ => fail("This should generate a HubCertCollection!")
    }

    hubCertCollection.allEntries.size shouldBe 2
    hubCertCollection.myEntry.privateKey.isDefined shouldBe true
    hubCertCollection.caEntry.privateKey.isDefined shouldBe false
    hubCertCollection.myEntry.aliases.first shouldBe "shrine-test"
    hubCertCollection.caEntry.aliases.first shouldBe "shrine-test-ca"
    hubCertCollection.caEntry.wasSignedBy(hubCertCollection.myEntry) shouldBe false
    hubCertCollection.myEntry.wasSignedBy(hubCertCollection.caEntry) shouldBe true
    //hubCertCollection.myEntry.verify(hubCertCollection.myEntry.sign(heyo).get, heyo) shouldBe true
    hubCertCollection.caEntry.verify(hubCertCollection.myEntry.sign(heyo).get, heyo) shouldBe true
    hubCertCollection.verifyBytes(hubCertCollection.signBytes(heyo), heyo) shouldBe true
  }
}
