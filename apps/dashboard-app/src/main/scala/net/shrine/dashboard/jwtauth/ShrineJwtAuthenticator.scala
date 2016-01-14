package net.shrine.dashboard.jwtauth

import java.io.ByteArrayInputStream
import java.security.{PublicKey, Key, PrivateKey}
import java.security.cert.{CertificateFactory, X509Certificate}
import java.util.Date

import io.jsonwebtoken.impl.TextCodec
import io.jsonwebtoken.{ClaimJwtException, Claims, SignatureAlgorithm, ExpiredJwtException, Jwts}
import net.shrine.crypto.{CertCollection, KeyStoreDescriptorParser, KeyStoreCertCollection}
import net.shrine.dashboard.DashboardConfigSource
import net.shrine.i2b2.protocol.pm.User
import net.shrine.log.Loggable
import net.shrine.protocol.Credential
import spray.http.HttpHeaders.{Authorization, `WWW-Authenticate`}
import spray.http.{HttpRequest, OAuth2BearerToken, HttpHeader, HttpChallenge}
import spray.routing.AuthenticationFailedRejection.{CredentialsMissing, CredentialsRejected}
import spray.routing.AuthenticationFailedRejection
import spray.routing.authentication._

import scala.concurrent.{Future, ExecutionContext}
import scala.util.{Success, Failure, Try}

/**
  * An Authenticator that uses Jwt in a Bearer header to authenticate. See http://jwt.io/introduction/ for what this is all about,
  * https://tools.ietf.org/html/rfc7519 for what it might include for claims.
  *
  * @author david 
  * @since 12/21/15
  */
object ShrineJwtAuthenticator extends Loggable {

  val config = DashboardConfigSource.config
  val certCollection: KeyStoreCertCollection = KeyStoreCertCollection.fromFileRecoverWithClassPath(KeyStoreDescriptorParser(config.getConfig("shrine.keystore")))

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

    OAuth2BearerToken(jwtsString)
  }

  def extractAuthorizationHeader(request: HttpRequest):Try[HttpHeader] = Try {
    case class NoAuthorizationHeaderException(request: HttpRequest) extends ShrineJwtException(s"No ${Authorization.name} header found in $request",missingCredentials)
    //noinspection ComparingUnrelatedTypes
    request.headers.find(_.name.equals(Authorization.name)).getOrElse{throw NoAuthorizationHeaderException(request)}
  }

  def extractJwtsStringAndCheckScheme(httpHeader: HttpHeader) = Try {
    val splitHeaderValue: Array[String] = httpHeader.value.trim.split(" ")
    if (splitHeaderValue.length != 2) {
      case class WrongNumberOfSegmentsException(httpHeader: HttpHeader) extends ShrineJwtException(s"Header had ${splitHeaderValue.length} space-delimited segments, not 2, in $httpHeader.",missingCredentials)
      throw new WrongNumberOfSegmentsException(httpHeader)
    }
    else if(splitHeaderValue(0) != BearerAuthScheme) {
      case class NotBearerAuthException(httpHeader: HttpHeader) extends ShrineJwtException(s"Expected ${BearerAuthScheme}, not ${splitHeaderValue(0)} in $httpHeader.",missingCredentials)
      throw new NotBearerAuthException(httpHeader)
    }
    else splitHeaderValue(1)
  }

  def extractJwtsClaims(jwtsString:String):Try[Claims] = Try {
    val jwtsClaims: Claims = Jwts.parser().setSigningKeyResolver(new SigningKeyResolverBridge()).parseClaimsJws(jwtsString).getBody
    jwtsClaims
  }

  //from https://groups.google.com/forum/#!topic/spray-user/5DBEZUXbjtw
  def authenticate(implicit ec: ExecutionContext): ContextAuthenticator[User] = { ctx =>

    Future {

      val attempt: Try[Authentication[User]] = for {
        header:HttpHeader <- extractAuthorizationHeader(ctx.request)
        jwtsString:String <- extractJwtsStringAndCheckScheme(header)
        jwtsClaims:Claims <- extractJwtsClaims(jwtsString)
      } yield {

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
                  else if (jwtsClaims.getIssuer == null) {
                    info(s"jwts from ${cert.getSubjectDN.getName} issuer is null")
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
      }
      //todo use a fold() in Scala 2.12
      attempt match {
        case Success(rightUser) => rightUser
        case Failure(x) =>  x match
        {
          case anticipated: ShrineJwtException => {
            info(s"Failed to authenticate due to ${anticipated.toString}",anticipated)
            anticipated.rejection
          }
          case fromJwts: ClaimJwtException => {
            info(s"Failed to authenticate due to ${fromJwts.toString}",fromJwts)
            rejectedCredentials
          }
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
          case unanticipated => {
            warn(s"Unanticipated ${unanticipated.toString} while authenticating ${ctx.request}",unanticipated)
            rejectedCredentials
          }
        }
      }
    }
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

    //check date on cert vs time. throws CertificateExpiredException or CertificateNotYetValidException for problems
    //todo skip this until you rebuild the certs used for testing                certificate.checkValidity(now)

    certificate.getPublicKey
  }

  def certForString(string: String): X509Certificate = {
    val certBytes = TextCodec.BASE64URL.decode(string)

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

abstract class ShrineJwtException(message:String,
                                  val rejection:Authentication[User] = ShrineJwtAuthenticator.rejectedCredentials,
                                  cause:Throwable = null) extends RuntimeException(message,cause)