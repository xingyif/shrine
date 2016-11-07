package net.shrine.crypto

import net.shrine.crypto2.BouncyKeyStoreCollection

/**
 * @author clint
 * @date Nov 22, 2013
 */
sealed trait TrustParam

object TrustParam {
  //NB: For Spring
  @Deprecated
  def forKeyStore(certs: KeyStoreCertCollection): SomeKeyStore = SomeKeyStore(certs)
  
  case object AcceptAllCerts extends TrustParam

  final case class SomeKeyStore(certs: KeyStoreCertCollection) extends TrustParam

  final case class BouncyKeyStore(certs: BouncyKeyStoreCollection) extends TrustParam
}