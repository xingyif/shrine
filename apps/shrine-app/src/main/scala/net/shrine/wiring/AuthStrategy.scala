package net.shrine.wiring

import com.typesafe.config.Config
import net.shrine.authentication.{AuthenticationType, Authenticator, PmAuthenticator}
import net.shrine.authorization.{AuthorizationType, StewardQueryAuthorizationService, QueryAuthorizationService, AllowsAllAuthorizationService}
import net.shrine.qep.AllowsAllAuthenticator
import net.shrine.client.Poster
import net.shrine.hms.authorization.HmsDataStewardAuthorizationService
import net.shrine.hms.authentication.EcommonsPmAuthenticator
import net.shrine.hms.authorization.JerseySheriffClient


/**
  * @author clint
  * @since Jul 1, 2014
  */
object AuthStrategy {

  import AuthenticationType._
  import AuthorizationType._

  def determineAuthenticator(authType: AuthenticationType, pmPoster: Poster): Authenticator = authType match {
    case NoAuthentication => AllowsAllAuthenticator
    case Pm => PmAuthenticator(pmPoster)
    case Ecommons => EcommonsPmAuthenticator(pmPoster)
    case _ => throw new Exception(s"Disallowed authentication type '$authType'")
  }

  def determineQueryAuthorizationService(qepConfig:Config,authType: AuthorizationType, shrineConfigBall: ShrineConfig, authenticator: Authenticator): QueryAuthorizationService = {
    authType match {
      case ShrineSteward => makeShrineStewardAuthorizationService(qepConfig)
      case HmsSteward => makeHmsStewardAuthorizationService(qepConfig,shrineConfigBall, authenticator)
      case NoAuthorization => AllowsAllAuthorizationService
      case _ => throw new Exception(s"Disallowed authorization type '$authType'")
    }
  }

  private def makeShrineStewardAuthorizationService(qepConfig:Config): QueryAuthorizationService = {
    val stewardConfig: Config = qepConfig.getConfig("shrineSteward")
    StewardQueryAuthorizationService(stewardConfig)
  }

  private def makeHmsStewardAuthorizationService(qepConfig:Config,shrineConfigBall: ShrineConfig, authenticator: => Authenticator): QueryAuthorizationService = {
    //todo put all this in JerseySheriffClient's apply
    //NB: Fail fast here, since on the fully-meshed HMS deployment, all nodes are expected to be
    //query entry points

    val queryEntryPointConfig = shrineConfigBall.queryEntryPointConfig.get

    val sheriffEndpointOption = queryEntryPointConfig.sheriffEndpoint
    val sheriffCredentialsOption = queryEntryPointConfig.sheriffCredentials

    //NB: Fail fast, HMS nodes need to use The Sheriff
    require(sheriffEndpointOption.isDefined, "Sheriff endpoint must be defined in shrine.conf")
    //NB: Fail fast, HMS nodes need to use The Sheriff
    require(sheriffCredentialsOption.isDefined, "Sheriff credentials must be defined in shrine.conf")

    val sheriffUrl = sheriffEndpointOption.get.url.toString

    val sheriffUsername = sheriffCredentialsOption.get.username
    val sheriffPassword = sheriffCredentialsOption.get.password

    val sheriffClient = JerseySheriffClient(sheriffUrl, sheriffUsername, sheriffPassword)

    HmsDataStewardAuthorizationService(sheriffClient, authenticator)
  }
}