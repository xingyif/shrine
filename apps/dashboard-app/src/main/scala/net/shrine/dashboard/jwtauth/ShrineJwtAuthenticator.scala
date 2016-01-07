package net.shrine.dashboard.jwtauth

import java.security.cert.X509Certificate

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
      val missingCredentials: Authentication[User] = Left(AuthenticationFailedRejection(CredentialsMissing, List(challengeHeader)))
      val rejectedCredentials: Authentication[User] =  Left(AuthenticationFailedRejection(CredentialsRejected,List(challengeHeader)))

      ctx.request.headers.find(_.name.equals(Authorization.name)).fold(missingCredentials) { (header: HttpHeader) =>

        //header should be "$ShrineJwtAuth0: $SignerSerialNumber: $JwtsString

        val splitHeaderValue: Array[String] = header.value.split(": ")
        if (splitHeaderValue.length == 3) {

          if (splitHeaderValue(0) == ShrineJwtAuth0) {
            //todo read the string and validate the user
            try {
              val certSerialNumber: BigInt = BigInt(splitHeaderValue(1))

              val config = DashboardConfigSource.config

              val shrineCertCollection: KeyStoreCertCollection = KeyStoreCertCollection.fromFileRecoverWithClassPath(KeyStoreDescriptorParser(config.getConfig("shrine.keystore")))

              shrineCertCollection.get(CertId(certSerialNumber.bigInteger)).fold{
                info(s"Cert serial number ${certSerialNumber.bigInteger} could not be found in the KeyStore.")
                rejectedCredentials
              } { (certificate: X509Certificate) =>

                val key = certificate.getPublicKey
                Jwts.parser().setSigningKey(key).parseClaimsJws(splitHeaderValue(2))

                val user = User(
                  fullName = certificate.getIssuerDN.getName, //todo maybe get something out of jwts
                  username = certificate.getSubjectDN.getName,
                  domain = "dashboard-to-dashboard",
                  credential = Credential("Dashboard credential", isToken = false),
                  params = Map(),
                  rolesByProject = Map()
                )
                Right(user)
              }
            } catch {
              case x:NumberFormatException => {
                info(s"Cert serial number could not be read as a BigInteger.",x)
                missingCredentials
              }
            }
          }
          else {
            info(s"Header did not start with $ShrineJwtAuth0 .")
            missingCredentials
          }
        }
        else {
          info(s"Header had ${splitHeaderValue.length} :-delimited segments, not 3. ")
          missingCredentials
        }
      }
    }
  }

}