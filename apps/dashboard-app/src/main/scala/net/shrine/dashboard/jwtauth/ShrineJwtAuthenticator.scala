package net.shrine.dashboard.jwtauth

import java.security.PublicKey

import io.jsonwebtoken.Jwts
import net.shrine.crypto.{KeyStoreDescriptorParser, KeyStoreCertCollection}
import net.shrine.dashboard.DashboardConfigSource
import net.shrine.i2b2.protocol.pm.User
import net.shrine.log.Loggable
import net.shrine.protocol.{CertId, Credential}
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

  val ShrineJwtAuth0 = "ShrineJwtAuth0"
  val challengeHeader:`WWW-Authenticate` = `WWW-Authenticate`(HttpChallenge(ShrineJwtAuth0, "dashboard-to-dashboard")) //todo hostname for cert

  //from https://groups.google.com/forum/#!topic/spray-user/5DBEZUXbjtw
  def theAuthenticator(implicit ec: ExecutionContext): ContextAuthenticator[User] = { ctx =>

    Future {
      val noAuthHeader: Authentication[User] = Left(AuthenticationFailedRejection(CredentialsMissing, List(challengeHeader)))
      ctx.request.headers.find(_.name.equals(Authorization.name)).fold(noAuthHeader) { (header: HttpHeader) =>

        //header should be "$ShrineJwtAuth0: $SignerSerialNumber: $JwtsString

        val splitHeaderValue: Array[String] = header.value.split(": ")
        //todo check length and reject if not == 3

        if (splitHeaderValue(0)==ShrineJwtAuth0) {
          //todo read the string and validate the user
          val certSerialNumber: BigInt = BigInt(splitHeaderValue(1)) //todo error if not a big int

          val config = DashboardConfigSource.config

          val shrineCertCollection: KeyStoreCertCollection = KeyStoreCertCollection.fromFileRecoverWithClassPath(KeyStoreDescriptorParser(config.getConfig("shrine.keystore")))

          val key: PublicKey = shrineCertCollection.get(CertId(certSerialNumber.bigInteger)).get.getPublicKey //todo getOrElse
          Jwts.parser().setSigningKey(key).parseClaimsJws(splitHeaderValue(2))

          val user = User(fullName = s"$certSerialNumber dashboard", //todo get and check DN or CN, use that here
            username = certSerialNumber.toString(), //todo get the request origin and use that here
            domain = "dashboard-to-dashboard",
            credential = Credential("fake dashboard credential", isToken = false),
            params = Map(),
            rolesByProject = Map()
          )
          Right(user)
        }
        else noAuthHeader
  //        Left(AuthenticationFailedRejection(CredentialsRejected,List(challengeHeader)))
      }
    }
  }

}