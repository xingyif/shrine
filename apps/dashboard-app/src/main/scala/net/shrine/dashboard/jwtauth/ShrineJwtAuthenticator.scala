package net.shrine.dashboard.jwtauth

import net.shrine.i2b2.protocol.pm.User
import net.shrine.log.Loggable
import net.shrine.protocol.Credential
import spray.http.HttpHeaders.{Authorization, `WWW-Authenticate`}
import spray.http.{HttpHeader, HttpChallenge}
import spray.routing.AuthenticationFailedRejection.{CredentialsMissing, CredentialsRejected}
import spray.routing.AuthenticationFailedRejection
import spray.routing.authentication._

import scala.concurrent.{Future, ExecutionContext}

/**
  * An Authenticator that uses Jwt in a ShrineJwt1 header to authenticate.
  *
  * @author david 
  * @since 12/21/15
  */
object ShrineJwtAuthenticator extends Loggable{

  val challengeHeader:`WWW-Authenticate` = `WWW-Authenticate`(HttpChallenge("ShrineAuth0", "Not Relevant"))

  //from https://groups.google.com/forum/#!topic/spray-user/5DBEZUXbjtw
  def theAuthenticator(implicit ec: ExecutionContext): ContextAuthenticator[User] = { ctx =>

    Future {

      val noAuthHeader: Authentication[User] = Left(AuthenticationFailedRejection(CredentialsMissing, List(challengeHeader)))
      ctx.request.headers.filter(_.name.equals(Authorization.name)).headOption.fold(noAuthHeader) { (header: HttpHeader) =>

        info(s"header is $header")

        val user = User(fullName = "fake dashboard",
          username = "fake dashboard",
          domain = "domain",
          credential = Credential("fake dashboard credentail", isToken = false),
          params = Map(),
          rolesByProject = Map()
        )

//        Left(AuthenticationFailedRejection(CredentialsRejected,List(challengeHeader)))

        Right(user)
      }
    }
  }

}