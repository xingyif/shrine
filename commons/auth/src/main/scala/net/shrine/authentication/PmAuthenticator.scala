package net.shrine.authentication

import net.shrine.client.Poster
import net.shrine.protocol.AuthenticationInfo
import net.shrine.i2b2.protocol.pm.User

/**
 * @author clint
 * @date Feb 26, 2014
 */
final case class PmAuthenticator(pmPoster: Poster) extends BasePmAuthenticator(pmPoster, PmAuthenticator.makeAuthenticationResult)

object PmAuthenticator {
  def makeAuthenticationResult(presentedAuthn: AuthenticationInfo, userFromPm: User): AuthenticationResult = {
    //If we got a valid response from the PM - if we got this far - we're authenticated
    AuthenticationResult.Authenticated(userFromPm.domain, userFromPm.username)
  }
}