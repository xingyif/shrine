package net.shrine.crypto

import java.math.BigInteger
import java.security.PrivateKey
import java.security.cert.X509Certificate

import net.shrine.crypto2.{BouncyKeyStoreCollection, CertCollectionAdapter}
import net.shrine.protocol.CertId
import net.shrine.util.ShouldMatchersForJUnit
import org.junit.Test

/**
 * @author clint
 * @since Dec 2, 2013
 */
final class KeyStoreCertCollectionTest extends ShouldMatchersForJUnit {
  private def bigInt(i: Int) = BigInteger.valueOf(i)

  private def bigInt(s: String) = new BigInteger(s)

  @Test
  def testInstanceMethods: Unit = doKeystoreTest(OldTestKeyStore.certCollection)

  @Test
  def testNoPrivateKeyAtSpecifiedAlias {
    val collectionThatShouldBeFound = KeyStoreCertCollection.fromClassPathResource(OldTestKeyStore.descriptor)

    val descriptorWithBogusAlias = OldTestKeyStore.descriptor.copy(privateKeyAlias = Some("bogus cert alias"))

    //Should throw, since there is no cert-with-private-key at the alias we specified
    intercept[Exception] {
      KeyStoreCertCollection.fromClassPathResource(descriptorWithBogusAlias)
    }
  }

  @Test
  def testNoPrivateKeyAlias {
    val descriptorWithBogusAlias = OldTestKeyStore.descriptor.copy(file = "shrine.keystore-one-private-key", privateKeyAlias = None)

    //Shouldn't throw, since there's only one private key - this key should be found and used
    KeyStoreCertCollection.fromClassPathResource(descriptorWithBogusAlias)
  }

  @Test
  def testMultiplePrivateKeys {
    {
      val descriptor = OldTestKeyStore.descriptor.copy(privateKeyAlias = None, file = "shrine.keystore.multiple-private-keys")

      //Should throw, since no private key alias was specified, and multiple private keys were found
      intercept[Exception] {
        KeyStoreCertCollection.fromClassPathResource(descriptor)
      }
    }
    
    {
      val descriptor = OldTestKeyStore.descriptor.copy(file = "shrine.keystore.multiple-private-keys")

      //Should work, since even though multiple private keys were found, a private key alias was specified
      val keyStore = KeyStoreCertCollection.fromClassPathResource(descriptor)
    }
  }

  private def doKeystoreTest(collection: CertCollection) {
    collection.isEmpty should be(false)

    collection.size should be(3)

    collection.get(CertId(bigInt(3))).get.getSerialNumber should equal(bigInt(3))

    collection.myCert.get.getSerialNumber should equal(bigInt(3))
    
    collection.myCertId.flatMap(collection.get).get.getSerialNumber should equal(bigInt(3))

    //val keystore = collection.asInstanceOf[KeyStoreCertCollection].keystore

    //keystore should not be (null)

    val caSerials = Set(bigInt("16398565510742424207"))
    
    val serials = Set(bigInt("1143048354"), bigInt("3"))
    
    val allSerials = caSerials ++ serials
    collection match {
      case CertCollectionAdapter(c) => c.allEntries.foreach(entry => println(s"HEY! \n${entry.certificateHolder.getSerialNumber}, ${entry.aliases.first}"))
      case _ => println("Not an adapter")
    }
    collection.caIds.map(_.serial).toSet should equal(caSerials)

    collection.ids.map(_.serial).toSet should equal(serials)

    collection.iterator.map(_.getSerialNumber).toSet should equal(allSerials)

//    collection.myKeyPair should equal {
//      val expectedPublicKey = collection.myCertId.flatMap(collection.get).get.getPublicKey
//
//      val expectedPrivateKey = keystore.getKey(TestKeystore.privateKeyAlias.get, TestKeystore.password.toCharArray).asInstanceOf[PrivateKey]
//
//      KeyPair(expectedPublicKey, expectedPrivateKey)
//    }
    
    val (caPrincipal, caCert) = collection.caCerts.head
    
    caCert.getSerialNumber should equal(caSerials.head)


    caPrincipal should equal(CertCollection.getIssuer(caCert))
  }

  private def doKeyStoreTestForBoth(descriptor: KeyStoreDescriptor) {
    val certCollection = KeyStoreCertCollection.fromFileRecoverWithClassPath(descriptor)

    doKeystoreTest(certCollection)
  }

  @Test
  def testSize {
    val collection = OldTestKeyStore.certCollection

    collection.size should be(3)
  }

  //NB: Also exercises fromStream()
  @Test
  def testFromFile {
    import KeyStoreCertCollection.fromFile
    doKeyStoreTestForBoth(OldTestKeyStore.certCollection.descriptor.copy(file = "src/test/resources/shrine.keystore"))

    intercept[Exception] {
      fromFile(OldTestKeyStore.certCollection.descriptor.copy(file = "sakfjalskflkasjflas.foo"))
    }
  }

  //NB: Also exercises fromStream()
  @Test
  def testFromClassPathResource {
    import KeyStoreCertCollection.fromClassPathResource

    doKeyStoreTestForBoth(OldTestKeyStore.certCollection.descriptor)


    intercept[Exception] {
      fromClassPathResource(OldTestKeyStore.certCollection.descriptor.copy(file = "sakfjalskflkasjflas.foo"))
    }
  }
}