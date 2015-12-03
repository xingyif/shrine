package net.shrine.service

import net.shrine.authentication.Authenticator
import net.shrine.authentication.AuthenticationResult.Authenticated
import net.shrine.protocol.AuthenticationInfo

/**
 * @author clint
 * @date Dec 13, 2013
 * 
 * This needs to be instantiatable, since Spring doesn't like Scala objects very much. :(
 */
class AllowsAllAuthenticator extends Authenticator {
  override def authenticate(authn: AuthenticationInfo) = Authenticated(authn.domain, authn.username) 
}


object AllowsAllAuthenticator extends AllowsAllAuthenticator