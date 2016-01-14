package net.shrine.dashboard.jwtauth

import java.io.ByteArrayInputStream
import java.security.{PublicKey, Key, PrivateKey}
import java.security.cert.{CertificateFactory, X509Certificate}
import java.util.Date

import io.jsonwebtoken.impl.TextCodec
import io.jsonwebtoken.{Claims, SignatureAlgorithm, ExpiredJwtException, Jwts}
import net.shrine.crypto.{CertCollection, KeyStoreDescriptorParser, KeyStoreCertCollection}
import net.shrine.dashboard.DashboardConfigSource
import net.shrine.i2b2.protocol.pm.User
import net.shrine.log.Loggable
import net.shrine.protocol.Credential
import spray.http.HttpHeaders.{Authorization, `WWW-Authenticate`}
import spray.http.{OAuth2BearerToken, HttpHeader, HttpChallenge}
import spray.routing.AuthenticationFailedRejection.{CredentialsMissing, CredentialsRejected}
import spray.routing.AuthenticationFailedRejection
import spray.routing.authentication._

import scala.concurrent.{Future, ExecutionContext}
import scala.util.Try

/**
  * An Authenticator that uses Jwt in a ShrineJwt1 header to authenticate. See http://jwt.io/introduction/ for what this is all about,
  * https://tools.ietf.org/html/rfc7519 for what it might include for claims.
  *
  * @author david 
  * @since 12/21/15
  */
object ShrineJwtAuthenticator extends Loggable {

  val config = DashboardConfigSource.config
  val certCollection: KeyStoreCertCollection = KeyStoreCertCollection.fromFileRecoverWithClassPath(KeyStoreDescriptorParser(config.getConfig("shrine.keystore")))

  //from https://groups.google.com/forum/#!topic/spray-user/5DBEZUXbjtw
  def authenticate(implicit ec: ExecutionContext): ContextAuthenticator[User] = { ctx =>

    Future {

      //noinspection ComparingUnrelatedTypes
      ctx.request.headers.find(_.name.equals(Authorization.name)).fold(missingCredentials) { (header: HttpHeader) =>

        //header should be "$ShrineJwtAuth0 $SignerSerialNumber,$JwtsString
        val splitHeaderValue: Array[String] = header.value.split(" ")
        if (splitHeaderValue.length == 2) {

          val authScheme = splitHeaderValue(0)
          val jwtsString = splitHeaderValue(1)

          if (authScheme == BearerAuthScheme) {
            try {
              val jwtsClaims: Claims = Jwts.parser().setSigningKeyResolver(new SigningKeyResolverBridge()).parseClaimsJws(jwtsString).getBody
              info(s"got claims $jwtsClaims")

              val now = new Date()
              if (jwtsClaims.getExpiration.before(now)) {
                info(s"jwts ${jwtsClaims.getExpiration} expired before now $now")
                rejectedCredentials
              }
              else {
                val cert = KeySource.certForString(Jwts.parser().setSigningKeyResolver(new SigningKeyResolverBridge()).parseClaimsJws(jwtsString).getHeader.getKeyId)

                certCollection.caCerts.get(CertCollection.getIssuer(cert)).fold{
                  info(s"Could not find a CA certificate with issuer DN ${cert.getIssuerDN}. Known CA cert aliases are ${certCollection.caCertAliases.mkString(",")}")
                  rejectedCredentials
                } { signerCert =>
                  def isSignedBy(cert: X509Certificate)(caPubKey: PublicKey): Boolean = Try { cert.verify(caPubKey); true }.getOrElse(false)
                  if(!isSignedBy(cert)(signerCert.getPublicKey)) {
                    info(s"cert $cert was not signed by $signerCert as claimed.")
                    rejectedCredentials
                  }
                  else if (jwtsClaims.getSubject == null) {
                    info(s"jwts from ${cert.getSubjectDN.getName} subject is null")
                    rejectedCredentials
                  }
                  else {
                    val user = User(
                      fullName = cert.getSubjectDN.getName,
                      username = jwtsClaims.getSubject,
                      domain = jwtsClaims.getIssuer,
                      credential = Credential(jwtsClaims.getIssuer, isToken = false),
                      params = Map(),
                      rolesByProject = Map()
                    )
                    Right(user)
                  }
                }
              }
            } catch {
              /*
              case x: CertificateExpiredException => {
                //todo will these even be thrown here? Get some identification here
                info(s"Cert expired.", x)
                rejectedCredentials
              }
              case x: CertificateNotYetValidException => {
                info(s"Cert not yet valid.", x)
                rejectedCredentials
              }
              */
              case x: ExpiredJwtException =>
                info(s"Jwt for todo expired.", x) //todo get some identification in here
                rejectedCredentials
            }
          }
          else {
            info(s"Header did not start with $BearerAuthScheme .")
            missingCredentials
          }
        }
        else {
          info(s"Header had ${splitHeaderValue.length} space-delimited segments, not 2. ")
          missingCredentials
        }
      }
    }
  }

  def createOAuthCredentials(user:User): OAuth2BearerToken = {

    val base64Cert = new String(TextCodec.BASE64URL.encode(certCollection.myCert.get.getEncoded))

    val key: PrivateKey = certCollection.myKeyPair.privateKey
    val expiration: Date = new Date(System.currentTimeMillis() + 30 * 1000) //good for 30 seconds
    val jwtsString = Jwts.builder().
        setHeaderParam("kid", base64Cert).
        setSubject(s"${user.username} at ${user.domain}").
        setIssuer(java.net.InetAddress.getLocalHost.getHostName).
        setExpiration(expiration).
        signWith(SignatureAlgorithm.RS512, key).
        compact()

    val token = OAuth2BearerToken(jwtsString)
    info(s"token is $token")

    token
  }

  val BearerAuthScheme = "Bearer"
  val challengeHeader: `WWW-Authenticate` = `WWW-Authenticate`(HttpChallenge(BearerAuthScheme, "dashboard-to-dashboard"))
  val missingCredentials: Authentication[User] = Left(AuthenticationFailedRejection(CredentialsMissing, List(challengeHeader)))
  val rejectedCredentials: Authentication[User] = Left(AuthenticationFailedRejection(CredentialsRejected, List(challengeHeader)))

}

class KeySource {}

object KeySource extends Loggable {

  def keyForString(string: String): Key = {
    val certificate =certForString(string)
    //todo validate cert with something like obtainAndValidateSigningCert

    info(s"Created cert $certificate")

    //check date on cert vs time. throws CertificateExpiredException or CertificateNotYetValidException for problems
    //todo skip this until you rebuild the certs used for testing                certificate.checkValidity(now)

    val key = certificate.getPublicKey
    info(s"got key $key")
    key
  }

  def certForString(string: String): X509Certificate = {
    val certBytes = TextCodec.BASE64URL.decode(string)

    info(s"Got cert bytes $string")

    val inputStream = new ByteArrayInputStream(certBytes)

    val certificate = try {
      CertificateFactory.getInstance("X.509").generateCertificate(inputStream).asInstanceOf[X509Certificate]
    }
    finally {
      inputStream.close()
    }
    certificate
  }
}