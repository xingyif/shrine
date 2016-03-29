package net.shrine.hms.authorization

import net.shrine.authentication.{AuthenticationResult, Authenticator}
import net.shrine.authorization.{AuthorizationResult, QueryAuthorizationService}
import net.shrine.log.Loggable
import net.shrine.problem.{AbstractProblem, ProblemSources}
import net.shrine.protocol.{AuthenticationInfo, ErrorResponse, ReadApprovedQueryTopicsRequest, ReadApprovedQueryTopicsResponse, RunQueryRequest}

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
      case None => Left(ErrorResponse(HMSNotAuthenticatedProblem(authn)))
      case Some(ecommonsUsername) =>
        val topics = sheriffClient.getApprovedEntries(ecommonsUsername)

        Right(ReadApprovedQueryTopicsResponse(topics))

    }
  }

  override def authorizeRunQueryRequest(request: RunQueryRequest): AuthorizationResult = {
    val authn = request.authn

    if (request.topicId.isEmpty) {
      AuthorizationResult.NotAuthorized(s"HMS queries require a topic id; couldn't authenticate user ${toDomainAndUser(authn)}")
    } else {
      authenticate(authn) match {
        case None => AuthorizationResult.NotAuthorized(s"Requested topic is not approved; couldn't authenticate user ${toDomainAndUser(authn)}")
        case Some(ecommonsUsername) =>
          sheriffClient.isAuthorized(ecommonsUsername, request.topicId.get, request.queryDefinition.toI2b2String)

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

case class HMSNotAuthenticatedProblem(authn: AuthenticationInfo) extends AbstractProblem(ProblemSources.Qep){
  override val summary = s"Can not authenticate ${authn.domain}:${authn.username}."

  override val description = s"Can not authenticate ${authn.domain}:${authn.username}."
}