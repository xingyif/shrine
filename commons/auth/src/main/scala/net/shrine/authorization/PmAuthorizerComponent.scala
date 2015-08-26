package net.shrine.authorization

import net.shrine.log.Loggable

import scala.util.Try
import net.shrine.client.HttpResponse
import net.shrine.i2b2.protocol.pm.GetUserConfigurationRequest
import net.shrine.i2b2.protocol.pm.User
import net.shrine.protocol.AuthenticationInfo
import net.shrine.protocol.ErrorResponse
import scala.util.control.NonFatal

/**
 * @author clint
 * @date Apr 5, 2013
 */
trait PmAuthorizerComponent { self: PmHttpClientComponent with Loggable =>

  import PmAuthorizerComponent._

  object Pm {
    def parsePmResult(authn: AuthenticationInfo)(httpResponse: HttpResponse): Try[Either[ErrorResponse, User]] = {
      User.fromI2b2(httpResponse.body).map(Right(_)).recoverWith {
        case NonFatal(e) => {
          debug(s"Couldn't extract a User from '$httpResponse'")

          Try(Left(ErrorResponse.fromI2b2(httpResponse.body)))
        }
      }.recover {
        case NonFatal(e) => {
          warn(s"Couldn't understand response from PM: '$httpResponse'", e)

          Left(ErrorResponse(s"Error authorizing ${authn.domain}:${authn.username}: ${e.getMessage}"))
        }
      }
    }

    def authorize(projectId: String, neededRoles: Set[String], authn: AuthenticationInfo): AuthorizationStatus = {
      val request = GetUserConfigurationRequest(authn)

      val responseAttempt = Try {
        debug(s"Authorizing with PM cell at ${pmPoster.url}")

        pmPoster.post(request.toI2b2String)
      }

      val authStatusAttempt = responseAttempt.flatMap(parsePmResult(authn)).map {
        case Right(user) => {
          val managerUserOption = for {
            roles <- user.rolesByProject.get(projectId)
            if neededRoles.forall(roles.contains)
          } yield user

          managerUserOption.map(Authorized(_)).getOrElse {
            NotAuthorized(s"User ${authn.domain}:${authn.username} does not have all the needed roles: ${neededRoles.map("'" + _ + "'").mkString(", ")} in the project '$projectId'")
          }
        }
        case Left(ErrorResponse(message)) => NotAuthorized(message)
      }

      authStatusAttempt.getOrElse {
        NotAuthorized(s"Error authorizing ${authn.domain}:${authn.username} with PM at ${pmPoster.url}")
      }
    }
  }
}

object PmAuthorizerComponent {
  sealed trait AuthorizationStatus

  case class Authorized(user: User) extends AuthorizationStatus

  case class NotAuthorized(reason: String) extends AuthorizationStatus {
    def toErrorResponse = ErrorResponse(reason)
  }
}