package net.shrine.crypto

import net.shrine.util.PeerToPeerModel

/**
 * @author clint
 * @date Nov 27, 2013
 */
object TestKeystore {
  val fileName = "shrine.keystore"
  
  val password = "chiptesting"
    
  val privateKeyAlias: Option[String] = Some("test-cert")
    
  val keyStoreType: KeyStoreType = KeyStoreType.JKS
  
  val caCertAliases = Seq("carra ca", "shrine-ca")
  
  lazy val descriptor = KeyStoreDescriptor(fileName, password, privateKeyAlias, caCertAliases, keyStoreType, PeerToPeerModel)
  
  lazy val certCollection = KeyStoreCertCollection.fromClassPathResource(descriptor)
  
  lazy val trustParam: TrustParam = TrustParam.SomeKeyStore(certCollection)
}