package net.shrine.crypto

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
  
  lazy val descriptor = KeyStoreDescriptor(fileName, password, privateKeyAlias, caCertAliases, SingleHubModel(false),
    Seq(RemoteSiteDescriptor("hub", Some("shrine-test-ca"), "localhost", "8080")), keyStoreType)
  
  lazy val certCollection = BouncyKeyStoreCollection.fromFileRecoverWithClassPath(descriptor)
  
  lazy val trustParam: TrustParam = TrustParam.BouncyKeyStore(certCollection)
}