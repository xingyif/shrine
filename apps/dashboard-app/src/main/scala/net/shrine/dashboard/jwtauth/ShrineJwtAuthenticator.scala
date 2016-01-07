package net.shrine.dashboard.jwtauth

import java.security.cert.{CertificateNotYetValidException, CertificateExpiredException, X509Certificate}
import java.util.Date

import io.jsonwebtoken.{ExpiredJwtException, Claims, Jwts}
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
  * An Authenticator that uses Jwt in a ShrineJwt1 header to authenticate. See http://jwt.io/introduction/ for what this is all about,
  * https://tools.ietf.org/html/rfc7519 for what it might include for claims.
  *
  * @author david 
  * @since 12/21/15
  */
object ShrineJwtAuthenticator extends Loggable{

  val ShrineJwtAuth0 = "ShrineJwtAuth0" //We can't use Authorization: Bearer because we have to know which public key to use to decrypt, and we want the caller to authenticate via PGP from the start
  val challengeHeader:`WWW-Authenticate` = `WWW-Authenticate`(HttpChallenge(ShrineJwtAuth0, "dashboard-to-dashboard")) //todo hostname for cert

  //from https://groups.google.com/forum/#!topic/spray-user/5DBEZUXbjtw
  def authemticate(implicit ec: ExecutionContext): ContextAuthenticator[User] = { ctx =>

    Future {
      val missingCredentials: Authentication[User] = Left(AuthenticationFailedRejection(CredentialsMissing, List(challengeHeader)))
      val rejectedCredentials: Authentication[User] =  Left(AuthenticationFailedRejection(CredentialsRejected,List(challengeHeader)))

      ctx.request.headers.find(_.name.equals(Authorization.name)).fold(missingCredentials) { (header: HttpHeader) =>

        //header should be "$ShrineJwtAuth0: $SignerSerialNumber: $JwtsString

        val splitHeaderValue: Array[String] = header.value.split(": ")
        if (splitHeaderValue.length == 3) {

          if (splitHeaderValue(0) == ShrineJwtAuth0) {
            try {
              val certSerialNumber: BigInt = BigInt(splitHeaderValue(1))

              val config = DashboardConfigSource.config

              val shrineCertCollection: KeyStoreCertCollection = KeyStoreCertCollection.fromFileRecoverWithClassPath(KeyStoreDescriptorParser(config.getConfig("shrine.keystore")))

              shrineCertCollection.get(CertId(certSerialNumber.bigInteger)).fold{
                info(s"Cert serial number ${certSerialNumber.bigInteger} could not be found in the KeyStore.")
                rejectedCredentials
              } { (certificate: X509Certificate) =>

                val now = new Date()
                //check date on cert vs time. throws CertificateExpiredException or CertificateNotYetValidException for problems
//todo skip this until you rebuild the certs used for testing                certificate.checkValidity(now)

                val key = certificate.getPublicKey
                val jwtsClaims: Claims = Jwts.parser().setSigningKey(key).parseClaimsJws(splitHeaderValue(2)).getBody

                //todo check serial number vs jwts iss
                if(jwtsClaims.getIssuer != splitHeaderValue(1)) {
                  info(s"jwts issuer ${jwtsClaims.getIssuer} does not match signing cert serial number ${splitHeaderValue(1)}")
                  rejectedCredentials
                }
                //todo check exp vs time
                else if (jwtsClaims.getExpiration.before(now)) {
                  info(s"jwts experation ${jwtsClaims.getExpiration} expired before now $now")
                  rejectedCredentials
                }
                else {
                  val user = User(
                    fullName = certificate.getSubjectDN.getName,
                    username = jwtsClaims.getSubject,
                    domain = "dashboard-to-dashboard",
                    credential = Credential("Dashboard credential", isToken = false),
                    params = Map(),
                    rolesByProject = Map()
                  )
                  Right(user)
                }
              }
            } catch {
              case x:NumberFormatException => {
                info(s"Cert serial number ${splitHeaderValue(1)} could not be read as a BigInteger.",x)
                missingCredentials
              }
              case x:CertificateExpiredException => {
                info(s"Cert ${splitHeaderValue(1)} expired.",x)
                rejectedCredentials
              }
              case x:CertificateNotYetValidException => {
                info(s"Cert ${splitHeaderValue(1)} not yet valid.",x)
                rejectedCredentials
              }
              case x:ExpiredJwtException => {
                info(s"Jwt from ${splitHeaderValue(1)} expired.",x)
                rejectedCredentials
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