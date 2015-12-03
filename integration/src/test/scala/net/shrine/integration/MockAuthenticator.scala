package net.shrine.integration

import net.shrine.authentication.Authenticator
import net.shrine.authentication.AuthenticationResult.Authenticated
import net.shrine.protocol.AuthenticationInfo

/**
 * @author clint
 * @date Nov 27, 2013
 */
object MockAuthenticator extends Authenticator {
  override def authenticate(authn: AuthenticationInfo) = Authenticated(authn.domain, authn.username) 
}