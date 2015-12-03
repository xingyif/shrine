package net.shrine.utilities.scanner

import net.shrine.authentication.Authenticator
import net.shrine.authentication.AuthenticationResult
import net.shrine.protocol.AuthenticationInfo

/**
 * @author clint
 * @date Jan 14, 2014
 */
object Authenticators {
  val neverWorks: Authenticator = new Authenticator {
    override def authenticate(authn: AuthenticationInfo): AuthenticationResult = AuthenticationResult.NotAuthenticated(authn.domain, authn.username, "blarg")
  }

  val alwaysWorks: Authenticator = new Authenticator {
    override def authenticate(authn: AuthenticationInfo): AuthenticationResult = AuthenticationResult.Authenticated(authn.domain, authn.username)
  }
}