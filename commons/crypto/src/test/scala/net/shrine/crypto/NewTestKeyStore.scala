package net.shrine.crypto

import net.shrine.crypto2.BouncyKeyStoreCollection
import net.shrine.util.{PeerToPeerModel, SingleHubModel}

/**
 * @author clint
 * @date Nov 27, 2013
 */
object NewTestKeyStore {
  val fileName = "crypto2/shrine-test.jks"
  
  val password = "justatestpassword"
    
  val privateKeyAlias: Option[String] = Some("shrine-test")
    
  val keyStoreType: KeyStoreType = KeyStoreType.JKS
  
  val caCertAliases = Seq("shrine-test-ca")
  
  lazy val descriptor = KeyStoreDescriptor(fileName, password, privateKeyAlias, caCertAliases, keyStoreType, SingleHubModel(false), Seq(RemoteSiteDescriptor("hub", Some("shrine-test-ca"), "localhost:8080")))
  
  lazy val certCollection = BouncyKeyStoreCollection.fromFileRecoverWithClassPath(descriptor)
  
  lazy val trustParam: TrustParam = TrustParam.BouncyKeyStore(certCollection)
}

object OldTestKeyStore {

  val fileName = "shrine.keystore"

  val password = "chiptesting"

  val privateKeyAlias: Option[String] = Some("test-cert")

  val keyStoreType: KeyStoreType = KeyStoreType.JKS

  val caCertAliases = Seq("carra ca", "shrine-ca")

  lazy val descriptor = KeyStoreDescriptor(fileName, password, privateKeyAlias, caCertAliases, keyStoreType, PeerToPeerModel)

  lazy val certCollection = KeyStoreCertCollection.fromClassPathResource(descriptor)

  lazy val trustParam: TrustParam = TrustParam.SomeKeyStore(certCollection)

}