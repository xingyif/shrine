package net.shrine.dashboard.jwtauth

import java.io.ByteArrayInputStream
import java.security.cert.{CertificateFactory, X509Certificate}
import java.security.{Key, Principal, PrivateKey}
import java.util.Date

import io.jsonwebtoken.impl.TextCodec
import io.jsonwebtoken.{ClaimJwtException, Claims, Jws, Jwts, SignatureAlgorithm}
import net.shrine.crypto.{DownStreamCertCollection, HubCertCollection, PeerCertCollection}
import net.shrine.dashboard.KeyStoreInfo
import net.shrine.i2b2.protocol.pm.User
import net.shrine.log.Loggable
import net.shrine.protocol.Credential
import spray.http.HttpHeaders.{Authorization, `WWW-Authenticate`}
import spray.http.{HttpChallenge, HttpHeader, HttpRequest, OAuth2BearerToken}
import spray.routing.AuthenticationFailedRejection
import spray.routing.AuthenticationFailedRejection.{CredentialsMissing, CredentialsRejected}
import spray.routing.authentication._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

/**
  * An Authenticator that uses Jwt in a Bearer header to authenticate. See http://jwt.io/introduction/ for what this is all about,
  * https://tools.ietf.org/html/rfc7519 for what it might include for claims.
  *
  * @author david 
  * @since 12/21/15
  */
object ShrineJwtAuthenticator extends Loggable {
  val certCollection = KeyStoreInfo.certCollection

  //from https://groups.google.com/forum/#!topic/spray-user/5DBEZUXbjtw
  def authenticate(implicit ec: ExecutionContext): ContextAuthenticator[User] = { ctx =>

    Future {

      val attempt: Try[Authentication[User]] = for {
        header:HttpHeader <- extractAuthorizationHeader(ctx.request)
        jwtsString:String <- extractJwtsStringAndCheckScheme(header)
        jwtsClaims <- extractJwtsClaims(jwtsString)
        cert: X509Certificate <- extractAndCheckCert(jwtsClaims)
        jwtsBody:Claims <- Try{jwtsClaims.getBody}
        jwtsSubject <- failIfNull(jwtsBody.getSubject,MissingRequiredJwtsClaim("subject",cert.getSubjectDN))
        jwtsIssuer <- failIfNull(jwtsBody.getSubject,MissingRequiredJwtsClaim("issuer",cert.getSubjectDN))
      } yield {
        val user = User(
          fullName = cert.getSubjectDN.getName,
          username = jwtsSubject,
          domain = jwtsIssuer,
          credential = Credential(jwtsIssuer, isToken = false),
          params = Map(),
          rolesByProject = Map()
        )
        Right(user)
      }
      //todo use a fold() in Scala 2.12
      attempt match {
        case Success(rightUser) => rightUser
        case Failure(x) =>  x match
        {
          case anticipated: ShrineJwtException =>
            info(s"Failed to authenticate due to ${anticipated.toString}",anticipated)
            anticipated.rejection

          case fromJwts: ClaimJwtException =>
            info(s"Failed to authenticate due to ${fromJwts.toString} while authenticating ${ctx.request}",fromJwts)
            rejectedCredentials

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
          case unanticipated =>
            warn(s"Unanticipated ${unanticipated.toString} while authenticating ${ctx.request}",unanticipated)
            rejectedCredentials
        }
      }
    }
  }

  def createOAuthCredentials(user:User, dnsName: String): OAuth2BearerToken = {
      val oauthEntry = certCollection match {
        case HubCertCollection(myEntry, _, _) => myEntry
        case DownStreamCertCollection(myEntry, _, _) => myEntry // Note: The DownStreamCertCollection will never be accepted
        case PeerCertCollection(_, _, sites) => sites.find(_.url == dnsName).get.entry.get
        // This looks really dangerous, but it's an invariant that all remote sites for PeerToPeer collections have entries.
      }
      val base64Cert: String = TextCodec.BASE64URL.encode(oauthEntry.cert.getEncoded)

      val key: PrivateKey = certCollection.myEntry.privateKey.get
      val expiration: Date = new Date(System.currentTimeMillis() + 30 * 1000) //good for 30 seconds
      val jwtsString = Jwts.builder().
        setHeaderParam("kid", base64Cert).
        setSubject(s"${user.username} at ${user.domain}").
        setIssuer(java.net.InetAddress.getLocalHost.getHostName). //todo is it OK for me to use issuer this way or should I use my own claim?
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
      case class NotBearerAuthException(httpHeader: HttpHeader) extends ShrineJwtException(s"Expected $BearerAuthScheme, not ${splitHeaderValue(0)} in $httpHeader.",missingCredentials)
      throw new NotBearerAuthException(httpHeader)
    }
    else splitHeaderValue(1)
  }

  def extractJwtsClaims(jwtsString:String): Try[Jws[Claims]] = Try {
    Jwts.parser().setSigningKeyResolver(new SigningKeyResolverBridge()).parseClaimsJws(jwtsString)
  }

  def extractAndCheckCert(jwtsClaims:Jws[Claims]): Try[X509Certificate] = Try {
    val cert = KeySource.certForString(jwtsClaims.getHeader.getKeyId)

    val issuingSite = jwtsClaims.getBody.getIssuer

    case class CertIssuerNotInCollectionException(issuingSite:String,issuer: Principal, aliases: Iterable[String]) extends ShrineJwtException(s"Could not find a certificate with issuer DN $issuer. Known cert aliases are ${aliases.mkString(",")}")

    //todo is this the right way to find a cert in the certCollection?
    certCollection match {
      case DownStreamCertCollection(_, caEntry, _)
        if caEntry.signed(cert)                           => cert
      case HubCertCollection(ca, _, _) if ca.signed(cert) => cert
      case PeerCertCollection(_, entries, _) if entries.exists(_.signed(cert))
                                                          => cert
      case px: PeerCertCollection                         => px.allEntries.find(_.signed(cert)).map(_.cert).getOrElse(
        throw CertIssuerNotInCollectionException(issuingSite, cert.getIssuerDN, px.allEntries.flatMap(_.aliases))
      )
      case dc: DownStreamCertCollection                   => throw CertIssuerNotInCollectionException(issuingSite, cert.getIssuerDN, dc.caEntry.aliases)
      case hc: HubCertCollection                          => throw CertIssuerNotInCollectionException(issuingSite,cert.getIssuerDN, hc.caEntry.aliases)
    }

//    debug(s"certCollection.caCerts.contains(${cert.getSubjectX500Principal}) is ${caEntry.cert.getSubjectX500Principal == cert.getSubjectX500Principal}")
//    certCollection.caCerts.get(cert.getSubjectX500Principal).fold{
//      //if not in the keystore, check that the issuer is available
//      val issuer: Principal = cert.getIssuerX500Principal
//      case class CertIssuerNotInCollectionException(issuingSite:String,issuer: Principal) extends ShrineJwtException(s"Could not find a CA certificate with issuer DN $issuer. Known CA cert aliases are ${certCollection.caCertAliases.mkString(",")}")
//      val signingCert = certCollection.caCerts.getOrElse(issuer,{throw CertIssuerNotInCollectionException(issuingSite,issuer)})
//
//      //verify that the cert was signed using the signingCert
//      //todo this can throw CertificateException, NoSuchAlgorithmException, InvalidKeyException, NoSuchProviderException, SignatureException
//      cert.verify(signingCert.getPublicKey)
//      //todo has cert expired?
//      info(s"${cert.getSubjectX500Principal} verified using $issuer from the KeyStore")
//      cert
//    }{ principal => //if the cert is in the certCollection then all is well
//      info(s"$principal is in the KeyStore")
//      cert
//    }
  }

  def failIfNull[E](e:E,t:Throwable):Try[E] = Try {
    if(e == null) throw t
    else e
  }

  case class MissingRequiredJwtsClaim(field:String,principal: Principal) extends ShrineJwtException(s"$field is null from ${principal.getName}")

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