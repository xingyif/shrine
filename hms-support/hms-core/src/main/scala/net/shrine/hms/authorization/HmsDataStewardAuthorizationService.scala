package net.shrine.hms.authorization

import net.shrine.log.Loggable

import scala.util.Try
import scala.util.control.NonFatal
import scala.xml.Utility
import scala.xml.XML
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter
import javax.ws.rs.core.MediaType.APPLICATION_JSON
import net.liftweb.json.DefaultFormats
import net.liftweb.json.JField
import net.liftweb.json.JValue
import net.liftweb.json.parse
import net.shrine.authorization.AuthorizationException
import net.shrine.authorization.QueryAuthorizationService
import net.shrine.client.HttpClient
import net.shrine.client.HttpResponse
import net.shrine.client.JerseyHttpClient
import net.shrine.crypto.TrustParam.AcceptAllCerts
import net.shrine.i2b2.protocol.pm.GetUserConfigurationRequest
import net.shrine.protocol.ApprovedTopic
import net.shrine.protocol.AuthenticationInfo
import net.shrine.protocol.ReadApprovedQueryTopicsRequest
import net.shrine.protocol.ReadApprovedQueryTopicsResponse
import net.shrine.protocol.RunQueryRequest
import net.shrine.util.XmlUtil
import net.shrine.i2b2.protocol.pm.User
import javax.ws.rs.core.MediaType
import net.shrine.authorization.AuthorizationResult
import net.shrine.client.Poster
import net.shrine.authorization.PmHttpClientComponent
import net.shrine.authorization.PmAuthorizerComponent
import net.shrine.authentication.Authenticator
import net.shrine.protocol.ErrorResponse
import net.shrine.authentication.AuthenticationResult
import net.shrine.client.HttpCredentials

/**
 * @author Bill Simons
 * @since 1/30/12
 * @see http://cbmi.med.harvard.edu
 * @see http://chip.org
 *       <p/>
 *       NOTICE: This software comes with NO guarantees whatsoever and is
 *       licensed as Lgpl Open Source
 * @see http://www.gnu.org/licenses/lgpl.html
 */
final case class HmsDataStewardAuthorizationService(
  sheriffClient: SheriffClient,
  authenticator: Authenticator) extends QueryAuthorizationService with Loggable {

  import net.shrine.hms.authorization.HmsDataStewardAuthorizationService._

  override def readApprovedEntries(request: ReadApprovedQueryTopicsRequest): Either[ErrorResponse, ReadApprovedQueryTopicsResponse] = {
    val authn = request.authn

    authenticate(authn) match {
      case None => Left(ErrorResponse(s"Couldn't authenticate user ${toDomainAndUser(authn)}"))
      case Some(ecommonsUsername) => {
        val topics = sheriffClient.getApprovedEntries(ecommonsUsername)

        Right(ReadApprovedQueryTopicsResponse(topics))
      }
    }
  }

  override def authorizeRunQueryRequest(request: RunQueryRequest): AuthorizationResult = {
    val authn = request.authn

    if (request.topicIdAndName.isEmpty) {
      AuthorizationResult.NotAuthorized(s"HMS queries require a topic id; couldn't authenticate user ${toDomainAndUser(authn)}")
    } else {
      authenticate(authn) match {
        case None => AuthorizationResult.NotAuthorized(s"Requested topic is not approved; couldn't authenticate user ${toDomainAndUser(authn)}")
        case Some(ecommonsUsername) => {
          val isAuthorized = sheriffClient.isAuthorized(ecommonsUsername, request.topicIdAndName.get._1, request.queryDefinition.toI2b2String)

          if (isAuthorized) { AuthorizationResult.Authorized }
          else { AuthorizationResult.NotAuthorized("Requested topic is not approved") }
        }
      }
    }
  }

  private def authenticate(authn: AuthenticationInfo): Option[String] = {
    val authenticationResult = authenticator.authenticate(authn)

    identifyEcommonsUsername(authenticationResult)
  }
}

object HmsDataStewardAuthorizationService {
  private def toDomainAndUser(authn: AuthenticationInfo): String = s"${authn.domain}:${authn.username}"

  def identifyEcommonsUsername(authenticationResult: AuthenticationResult): Option[String] = authenticationResult match {
    case AuthenticationResult.Authenticated(_, ecommonsUsername) => Option(ecommonsUsername)
    case _ => None
  }
}