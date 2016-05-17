package net.shrine.authentication

import com.typesafe.config.Config
import net.shrine.authorization.steward.{qepRole, stewardRole}
import net.shrine.client.{EndpointConfig, JerseyHttpClient, Poster}
import net.shrine.crypto.{KeyStoreCertCollection, KeyStoreDescriptor, KeyStoreDescriptorParser, TrustParam}
import net.shrine.i2b2.protocol.pm.{BadUsernameOrPasswordException, GetUserConfigurationRequest, PmUserWithoutProjectException, User}
import net.shrine.log.Loggable
import net.shrine.protocol.{AuthenticationInfo, Credential}
import spray.http.HttpChallenge
import spray.http.HttpHeaders.`WWW-Authenticate`
import spray.routing.AuthenticationFailedRejection.CredentialsRejected
import spray.routing.{AuthenticationFailedRejection, RejectionError}
import spray.routing.authentication.{BasicAuth, UserPass}
import spray.routing.directives.AuthMagnet

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future, blocking}

/**
 * @author david 
 * @since 3/16/15
 */
trait UserSource {

  def authenticateUser(userPass: Option[UserPass]): Future[Option[User]]

  val challengeHeader:`WWW-Authenticate` = `WWW-Authenticate`(HttpChallenge("BasicCustom", "Realm"))
//  val challengeHeader:`WWW-Authenticate` = `WWW-Authenticate`(HttpChallenge("Basic", "Realm"))
}

//See http://www.tecnoguru.com/blog/2014/07/07/implementing-http-basic-authentication-with-spray/ for an explanation
case class UserAuthenticator(config:Config) extends Loggable {

  val userSource:UserSource = config.getString("shrine.authenticate.usersource.type") match {
    case PmUserSource.configName => PmUserSource(config)
    case ConfigUserSource.configName => ConfigUserSource(config)
    case x => throw new IllegalStateException(s"The config value for 'shrine.authenticate.usersource.type' must be either ${PmUserSource.configName} for actual use or ${ConfigUserSource.configName} for testing. '$x' cannot be used.")
  }

  val realm = config.getString("shrine.authenticate.realm")

  def basicUserAuthenticator(implicit ec: ExecutionContext): AuthMagnet[User] = {

    def authenticator(userPass: Option[UserPass]): Future[Option[User]] = userSource.authenticateUser(userPass)

    BasicAuth((a:Option[UserPass]) => authenticator(a), realm = realm)
  }


}

case class PmUserSource(config:Config) extends UserSource with Loggable {

  val domain = config.getString("shrine.authenticate.usersource.domain")

  def authenticateUser(userPassOption: Option[UserPass]): Future[Option[User]] = Future {

    val noUser:Option[User] = None
    userPassOption.fold(noUser)(userPass => {
      val requestString = GetUserConfigurationRequest(AuthenticationInfo(domain,
        username = userPass.user,
        credential = Credential(userPass.pass, isToken = false))).toI2b2String
      val httpResponse = blocking {
        pmPoster.post(requestString)
      }

      if (httpResponse.statusCode >= 400) {
        val message = s"HttpResponse status is ${httpResponse.statusCode} from PM via ${pmPoster.url} for ${userPass.user}." // todo log  Full response is ${httpResponse}"
        warn(message)
        throw new IllegalStateException(message)
      }

      try {
        val user = User.fromI2b2(httpResponse.body).get
        Some(user)
      } catch {
        case x: BadUsernameOrPasswordException => {
          info(s"PM at ${pmPoster.url} found no (user,password) combination for ${userPass.user}.", x)
          throw RejectionError(AuthenticationFailedRejection(CredentialsRejected, List(challengeHeader)))
        }
        case x: PmUserWithoutProjectException => {
          warn(s"PM at ${pmPoster.url} found no projects for ${userPass.user}.", x)
          throw RejectionError(AuthenticationFailedRejection(CredentialsRejected, List(challengeHeader)))
        }
      }
    }
  )}

  lazy val pmPoster: Poster = {
    val pmEndpoint: EndpointConfig = EndpointConfig(config.getConfig("shrine.pmEndpoint"))
    import TrustParam.{AcceptAllCerts, SomeKeyStore}

    val trustParam =  if (pmEndpoint.acceptAllCerts) AcceptAllCerts
    else {
      val keyStoreDescriptor:KeyStoreDescriptor = KeyStoreDescriptorParser.apply(config.getConfig("shrine.keystore"))
      val keystoreCertCollection: KeyStoreCertCollection = KeyStoreCertCollection.fromFileRecoverWithClassPath(keyStoreDescriptor)
      SomeKeyStore(keystoreCertCollection)
    }

    val httpClient = JerseyHttpClient(trustParam, pmEndpoint.timeout)
    Poster(pmEndpoint.url.toString, httpClient)
  }

}

object PmUserSource {
  val configName = getClass.getSimpleName.dropRight(1)
}

case class ConfigUserSource(config:Config) extends UserSource {

  val prefix = "shrine.authenticate.usersource"

  val subconfig = config.getConfig(prefix)

  import scala.collection.JavaConverters._
  val roles = subconfig.entrySet().asScala.
    filterNot(x => x.getKey == "type").
    filterNot(x => x.getKey == "domain").
    map(x => x.getKey.take(x.getKey.indexOf(".")))

  //x:Entry[String,ConfigValue]
  val rolesToUserNames: Map[String, String] = roles.map(x => (
    x,
    subconfig.getConfig(x).getString("username")
    )).toMap

  val userNamesToPasswords: Map[String, String] = rolesToUserNames.map(x => (
    x._2,
    subconfig.getConfig(x._1).getString("password")
    ))

  lazy val qepUserName = rolesToUserNames("qep")
  lazy val qepPassword = userNamesToPasswords(qepUserName)
  lazy val researcherUserName = rolesToUserNames("researcher")
  lazy val researcherPassword = userNamesToPasswords(researcherUserName)
  lazy val stewardUserName = rolesToUserNames("steward")
  lazy val stewardPassword = userNamesToPasswords(stewardUserName)

  def authenticateUser(userPass: Option[UserPass]): Future[Option[User]] = Future {

    val noUser:Option[User] = None
    userPass.fold(noUser)(up =>{

      if (userNamesToPasswords(up.user) == up.pass) {
        val params:Map[String,String] = if (up.user == "qep") Map(qepRole -> "true")
        else if (up.user == stewardUserName) Map(stewardRole -> "true")
        else Map.empty
        val user: User = User(fullName = up.user,
          username = up.user,
          domain = "domain",
          credential = Credential(up.pass,isToken = false),
          params = params,
          rolesByProject = Map())
        Some(user)
      }
      else throw RejectionError(AuthenticationFailedRejection(CredentialsRejected,List(challengeHeader)))
    })
  }
}

object ConfigUserSource {
  val configName = getClass.getSimpleName.dropRight(1)
}