package net.shrine.crypto2

/**
 * @author clint
 * @date Nov 22, 2013
 */
sealed trait TrustParam

object TrustParam {

  case object AcceptAllCerts extends TrustParam

  final case class BouncyKeyStore(certs: BouncyKeyStoreCollection) extends TrustParam
}