package net.shrine.crypto

/**
 * @author clint
 * @since Dec 4, 2014
 */
final case class SigningCertStrategy private (name: String, description: String, attachSigningCert: Boolean)

object SigningCertStrategy {
  val DontAttach = SigningCertStrategy("DontAttach", "Don't Attach Signing Cert", false)
  val Attach = SigningCertStrategy("Attach", "Attach Signing Cert", true)
}