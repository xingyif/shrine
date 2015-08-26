package net.shrine.crypto

/**
 * @author clint
 * @date Nov 22, 2013
 */
sealed trait TrustParam

object TrustParam {
  //NB: For Spring
  @Deprecated
  def forKeyStore(certs: KeyStoreCertCollection): SomeKeyStore = SomeKeyStore(certs)
  
  final case object AcceptAllCerts extends TrustParam

  final case class SomeKeyStore(certs: KeyStoreCertCollection) extends TrustParam
}