package net.shrine.hms.authentication

import net.shrine.i2b2.protocol.pm.GetUserConfigurationRequest
import net.shrine.i2b2.protocol.pm.User
import net.shrine.client.HttpClient
import net.shrine.client.HttpResponse
import scala.util.Try
import net.shrine.authentication.AuthenticationResult
import net.shrine.authentication.Authenticator
import scala.util.control.NonFatal
import net.shrine.util.XmlDateHelper
import net.shrine.client.Poster
import net.shrine.protocol.AuthenticationInfo
import net.shrine.protocol.Credential
import net.shrine.authentication.BasePmAuthenticator

/**
 * @author Bill Simons
 * @date 3/7/12
 * @link http://cbmi.med.harvard.edu
 * @link http://chip.org
 *       <p/>
 *       NOTICE: This software comes with NO guarantees whatsoever and is
 *       licensed as Lgpl Open Source
 * @link http://www.gnu.org/licenses/lgpl.html
 */
final case class EcommonsPmAuthenticator(pmPoster: Poster) extends BasePmAuthenticator(pmPoster, EcommonsPmAuthenticator.doAuthentication)

object EcommonsPmAuthenticator {
  private def doAuthentication(presentedAuthn: AuthenticationInfo, userFromPm: User): AuthenticationResult = {
    userFromPm.ecommonsUsername match {
      case Some(ecommonsUsername) => AuthenticationResult.Authenticated(presentedAuthn.domain, ecommonsUsername)
      case None => AuthenticationResult.NotAuthenticated(presentedAuthn.domain, presentedAuthn.username, s"No ecommons id for user ${presentedAuthn.domain}:${presentedAuthn.username}")
    }
  }
}