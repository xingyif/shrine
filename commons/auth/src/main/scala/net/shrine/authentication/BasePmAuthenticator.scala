package net.shrine.authentication

import net.shrine.client.Poster
import net.shrine.i2b2.protocol.pm.GetUserConfigurationRequest
import net.shrine.client.HttpResponse
import net.shrine.protocol.AuthenticationInfo
import net.shrine.protocol.Credential
import net.shrine.i2b2.protocol.pm.User
import scala.util.control.NonFatal
import net.shrine.util.StackTrace
import scala.util.Try

/**
 * @author clint
 * @since Feb 26, 2014
 */
abstract class BasePmAuthenticator(pmPoster: Poster, makeAuthenticationResult: (AuthenticationInfo, User) => AuthenticationResult) extends Authenticator {

  final override def authenticate(authn: AuthenticationInfo): AuthenticationResult = {
    val httpResponseString = Try {
      val requestString = GetUserConfigurationRequest(authn).toI2b2String

      val httpResponse = pmPoster.post(requestString)

      httpResponse.body
    }

    val userFromPm = httpResponseString.flatMap(User.fromI2b2)

    val result = userFromPm.map { user =>
      makeAuthenticationResult(authn, user)
    }.recover {
      case NonFatal(e) => {
        val AuthenticationInfo(domain, username, _) = authn

        AuthenticationResult.NotAuthenticated(domain, username, s"Failed to certify user $domain:$username, $e, ${StackTrace.stackTraceAsString(e)}")
      }
    }
    
    result.get
  }
}