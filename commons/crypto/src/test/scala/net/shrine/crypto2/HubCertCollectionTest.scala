package net.shrine.crypto2

import net.shrine.crypto.{KeyStoreDescriptor, KeyStoreType, TestKeystore}
import net.shrine.util.SingleHubModel
import org.junit.runner.RunWith
import org.scalatest.{FlatSpec, Matchers, ShouldMatchers}
import org.scalatest.junit.JUnitRunner

/**
  * Created by ty on 11/1/16.
  */
@RunWith(classOf[JUnitRunner])
class HubCertCollectionTest extends FlatSpec with Matchers {
  val descriptor = KeyStoreDescriptor("crypto2/shrine-test.jks", "justatestpassword", None, Nil, KeyStoreType.JKS, SingleHubModel)
  val hubCertCollection = BouncyKeyStoreCollection.fromFileRecoverWithClassPath(descriptor)

  hubCertCollection.allEntries.size shouldBe 2

}
