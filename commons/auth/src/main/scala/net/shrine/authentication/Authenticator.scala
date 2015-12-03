package net.shrine.authentication

import net.shrine.protocol.AuthenticationInfo

/**
 * @author clint
 * @date Dec 12, 2013
 */
trait Authenticator {
  def authenticate(authn: AuthenticationInfo): AuthenticationResult
}