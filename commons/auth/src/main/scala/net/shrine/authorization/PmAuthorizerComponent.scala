package net.shrine.authorization

import net.shrine.log.Loggable
import net.shrine.problem.{LoggingProblemHandler, Problem, ProblemSources, AbstractProblem, ProblemDigest}

import scala.util.{Failure, Success, Try}
import net.shrine.client.HttpResponse
import net.shrine.i2b2.protocol.pm.GetUserConfigurationRequest
import net.shrine.i2b2.protocol.pm.User
import net.shrine.protocol.AuthenticationInfo
import net.shrine.protocol.ErrorResponse
import scala.util.control.NonFatal

/**
 * @author clint
 * @since Apr 5, 2013
 */
trait PmAuthorizerComponent { self: PmHttpClientComponent with Loggable =>

  import PmAuthorizerComponent._

  //noinspection RedundantBlock
  object Pm {
    def parsePmResult(authn: AuthenticationInfo)(httpResponse: HttpResponse): Try[Either[ErrorResponse, User]] = {
      User.fromI2b2(httpResponse.body).map(Right(_)).recoverWith {
        case NonFatal(e) => {
          debug(s"Couldn't extract a User from '$httpResponse'")

          Try(Left(ErrorResponse.fromI2b2(httpResponse.body)))
        }
      }.recover {
        case NonFatal(e) => {
          val problem = CouldNotInterpretResponseFromPmCell(pmPoster.url,authn,httpResponse,e)
          LoggingProblemHandler.handleProblem(problem)
          Left(ErrorResponse(problem.summary,Some(problem)))
        }
      }
    }

    def authorize(projectId: String, neededRoles: Set[String], authn: AuthenticationInfo): AuthorizationStatus = {
      val request = GetUserConfigurationRequest(authn)

      val responseAttempt: Try[HttpResponse] = Try {
        debug(s"Authorizing with PM cell at ${pmPoster.url}")

        pmPoster.post(request.toI2b2String)
      }

      val authStatusAttempt: Try[AuthorizationStatus with Product with Serializable] = responseAttempt.flatMap(parsePmResult(authn)).map {
        case Right(user) => {
          val managerUserOption = for {
            roles <- user.rolesByProject.get(projectId)
            if neededRoles.forall(roles.contains)
          } yield user

          managerUserOption.map(Authorized).getOrElse {
            NotAuthorized(MissingRequiredRoles(projectId,neededRoles,authn))
          }
        }
        case Left(errorResponse) => {
          //todo remove when ErrorResponse gets its message
          info(s"ErrorResponse message '${errorResponse.errorMessage}' may not have carried through to the NotAuthorized object")
          NotAuthorized(errorResponse.problemDigest)
        }
      }

      authStatusAttempt match {
        case Success(s) => s
        case Failure(x) => NotAuthorized(CouldNotReachPmCell(pmPoster.url,authn,x))
      }
    }
  }
}

object PmAuthorizerComponent {
  sealed trait AuthorizationStatus

  case class Authorized(user: User) extends AuthorizationStatus

  case class NotAuthorized(problemDigest: ProblemDigest) extends AuthorizationStatus {
    def toErrorResponse = ErrorResponse(problemDigest.summary,problemDigest)
  }

  object NotAuthorized {
    def apply(problem:Problem):NotAuthorized = NotAuthorized(problem.toDigest)
  }
}

case class MissingRequiredRoles(projectId: String, neededRoles: Set[String], authn: AuthenticationInfo) extends AbstractProblem(ProblemSources.Qep) {
  override val summary: String = s"User ${authn.domain}:${authn.username} is missing roles in project '$projectId'"

  override val description:String = s"User ${authn.domain}:${authn.username} does not have all the needed roles: ${neededRoles.map("'" + _ + "'").mkString(", ")} in the project '$projectId'"
}

case class CouldNotReachPmCell(pmUrl:String,authn: AuthenticationInfo,x:Throwable) extends AbstractProblem(ProblemSources.Qep) {
  override val throwable = Some(x)
  override val summary: String = s"Could not reach PM cell."
  override val description:String = s"Shrine encountered ${throwable.get} while attempting to reach the PM cell at $pmUrl for ${authn.domain}:${authn.username}."
}

case class CouldNotInterpretResponseFromPmCell(pmUrl:String,authn: AuthenticationInfo,httpResponse: HttpResponse,x:Throwable) extends AbstractProblem(ProblemSources.Qep) {
  override val throwable = Some(x)
  override def summary: String = s"Could not interpret response from PM cell."

  override def description: String = s"Shrine could not interpret the response from the PM cell at ${pmUrl} for ${authn.domain}:${authn.username}: due to ${throwable.get}"

  override val detailsXml = <details>
                              Response is {httpResponse}
                              {throwableDetail.getOrElse("")}
                            </details>
}