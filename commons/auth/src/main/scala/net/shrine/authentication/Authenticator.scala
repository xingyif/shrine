package net.shrine.authentication

import net.shrine.protocol.AuthenticationInfo

/**
 * @author clint
 * @since Dec 12, 2013
 */
trait Authenticator {
  def authenticate(authn: AuthenticationInfo): AuthenticationResult
}