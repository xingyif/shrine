package net.shrine.dashboard.jwtauth

import java.io.ByteArrayInputStream
import java.security.{Key, PrivateKey}
import java.security.cert.{CertificateFactory, CertificateNotYetValidException, CertificateExpiredException, X509Certificate}
import java.util.Date

import io.jsonwebtoken.impl.TextCodec
import io.jsonwebtoken.{Claims, SignatureAlgorithm, ExpiredJwtException, Jwts}
import net.shrine.crypto.{KeyStoreDescriptorParser, KeyStoreCertCollection}
import net.shrine.dashboard.DashboardConfigSource
import net.shrine.i2b2.protocol.pm.User
import net.shrine.log.Loggable
import net.shrine.protocol.Credential
import spray.http.HttpHeaders.{RawHeader, Authorization, `WWW-Authenticate`}
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
  val challengeHeader:`WWW-Authenticate` = `WWW-Authenticate`(HttpChallenge(ShrineJwtAuth0, "dashboard-to-dashboard"))

  //from https://groups.google.com/forum/#!topic/spray-user/5DBEZUXbjtw
  def authenticate(implicit ec: ExecutionContext): ContextAuthenticator[User] = { ctx =>

    Future {
      val missingCredentials: Authentication[User] = Left(AuthenticationFailedRejection(CredentialsMissing, List(challengeHeader)))
      val rejectedCredentials: Authentication[User] =  Left(AuthenticationFailedRejection(CredentialsRejected,List(challengeHeader)))

      ctx.request.headers.find(_.name.equals(Authorization.name)).fold(missingCredentials) { (header: HttpHeader) =>

        //header should be "$ShrineJwtAuth0 $SignerSerialNumber,$JwtsString
        val splitHeaderValue: Array[String] = header.value.split(" ")
        if (splitHeaderValue.length == 2) {

          val authScheme = splitHeaderValue(0)
          val jwtsString = splitHeaderValue(1)

            if (authScheme == ShrineJwtAuth0) {
              try {

                  val now = new Date()
                val jwtsClaims: Claims = Jwts.parser().setSigningKeyResolver(new SigningKeyResolverBridge()).parseClaimsJws(jwtsString).getBody

                  info(s"got claims $jwtsClaims")
                  if (jwtsClaims.getExpiration.before(now)) {
                    info(s"jwts experation ${jwtsClaims.getExpiration} expired before now $now")
                    rejectedCredentials
                  }
                  else {
                    val user = User(
                      fullName = jwtsClaims.getSubject,//todo fill in with something useful like certificate.getSubjectDN.getName,
                      username = jwtsClaims.getSubject,
                      domain = "dashboard-to-dashboard",
                      credential = Credential("Dashboard credential", isToken = false),
                      params = Map(),
                      rolesByProject = Map()
                    )
                    Right(user)
                  }
              } catch {
                case x: CertificateExpiredException => { //todo will these even be thrown here? Get some identification here
                  info(s"Cert expired.", x)
                  rejectedCredentials
                }
                case x: CertificateNotYetValidException => {
                  info(s"Cert not yet valid.", x)
                  rejectedCredentials
                }
                case x: ExpiredJwtException => {
                  info(s"Jwt for todo expired.", x) //todo get some identification in here
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
          info(s"Header had ${splitHeaderValue.length} space-delimited segments, not 2. ")
          missingCredentials
        }
      }
    }
  }

  def createAuthHeader:HttpHeader = {
    val config = DashboardConfigSource.config
    val shrineCertCollection: KeyStoreCertCollection = KeyStoreCertCollection.fromFileRecoverWithClassPath(KeyStoreDescriptorParser(config.getConfig("shrine.keystore")))

    //todo instead use shrineCertCollection.myCert.getEncoded as a Base64 byte array. Use that as the prefix

    val signerSerialNumber = shrineCertCollection.myCertId.get.serial

    val base64Cert = new String(TextCodec.BASE64URL.encode(shrineCertCollection.myCert.get.getEncoded))

    val key: PrivateKey = shrineCertCollection.myKeyPair.privateKey
    val expiration:Date = new Date(System.currentTimeMillis() + 30 * 1000) //good for 30 seconds
    val jwtsString = Jwts.builder().
                            setHeaderParam("kid",base64Cert).
                            setIssuer(signerSerialNumber.toString()).
                            setSubject(java.net.InetAddress.getLocalHost.getHostName).
                            setExpiration(expiration).
                          signWith(SignatureAlgorithm.RS512, key).
                          compact()
    //todo start here. investigate raw header problems.
    val header = RawHeader(Authorization.name,s"$ShrineJwtAuth0 $jwtsString")
    info(s"header is $header")

    header
  }

}

class KeySource{}
object KeySource extends Loggable {

  def keyForString(string:String):Key = {
    //todo instead decrypt with the cert from the header via something like obtainAndValidateSigningCert()
    val certBytes = TextCodec.BASE64URL.decode(string)

    info(s"Got cert bytes $string")

    val inputStream = new ByteArrayInputStream(certBytes)

    val certificate = try { CertificateFactory.getInstance("X.509").generateCertificate(inputStream).asInstanceOf[X509Certificate] }
    finally { inputStream.close() }
    //todo validate cert

    info(s"Created cert $certificate")

    val now = new Date()
    //check date on cert vs time. throws CertificateExpiredException or CertificateNotYetValidException for problems
    //todo skip this until you rebuild the certs used for testing                certificate.checkValidity(now)

    val key = certificate.getPublicKey
    info(s"got key $key")
    key
  }
}