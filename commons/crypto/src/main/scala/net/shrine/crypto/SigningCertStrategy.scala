package net.shrine.crypto

/**
  * @author clint
  * @since Dec 4, 2014
  */
final case class SigningCertStrategy private (name: String, description: String, attachSigningCert: Boolean)
// TODO: DELETE THIS ONCE WE CAN REMOVE CERTID FROM THE XML BROADCAST
object SigningCertStrategy {
  val DontAttach = SigningCertStrategy("DontAttach", "Don't Attach Signing Cert", attachSigningCert = false)
  val Attach = SigningCertStrategy("Attach", "Attach Signing Cert", attachSigningCert = true)
}