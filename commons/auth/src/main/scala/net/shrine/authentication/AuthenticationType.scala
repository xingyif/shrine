package net.shrine.authentication

import net.shrine.util.SEnum

/**
 * @author clint
 * @since Jul 1, 2014
 */
final case class AuthenticationType private (name: String) extends AuthenticationType.Value

object AuthenticationType extends SEnum[AuthenticationType] {
  val Ecommons = AuthenticationType("ecommons")
  val Pm = AuthenticationType("pm")
  val NoAuthentication = AuthenticationType("none")
}
