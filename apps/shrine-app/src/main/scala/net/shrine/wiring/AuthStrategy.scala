package net.shrine.wiring

import net.shrine.authorization.steward.StewardConfig
import net.shrine.config.Keys
import net.shrine.authentication.{AuthenticationType, Authenticator, PmAuthenticator}
import net.shrine.authorization.{AuthorizationType, StewardQueryAuthorizationService, QueryAuthorizationService, AllowsAllAuthorizationService}
import net.shrine.service.AllowsAllAuthenticator
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

  def determineQueryAuthorizationService(authType: AuthorizationType, shrineConfig: ShrineConfig, authenticator: Authenticator): QueryAuthorizationService = {
    authType match {
      case ShrineSteward => makeShrineStewardAuthorizationService(shrineConfig)
      case HmsSteward => makeHmsStewardAuthorizationService(shrineConfig, authenticator)
      case NoAuthorization => AllowsAllAuthorizationService
      case _ => throw new Exception(s"Disallowed authorization type '$authType'")
    }
  }

  private def makeShrineStewardAuthorizationService(shrineConfig: ShrineConfig): QueryAuthorizationService = {
    require(shrineConfig.queryEntryPointConfig.isDefined, s"${Keys.queryEntryPoint} section must be defined in shrine.conf")
    val queryEntryPointConfig = shrineConfig.queryEntryPointConfig.get


    require(queryEntryPointConfig.stewardConfig.isDefined, s"${Keys.queryEntryPoint}.shrineSteward section must be defined in shrine.conf")
    val stewardConfig: StewardConfig = queryEntryPointConfig.stewardConfig.get

    StewardQueryAuthorizationService(
      qepUserName = stewardConfig.qepUserName,
      qepPassword = stewardConfig.qepPassword,
      stewardBaseUrl = stewardConfig.stewardBaseUrl)
  }

  private def makeHmsStewardAuthorizationService(shrineConfig: ShrineConfig, authenticator: => Authenticator): QueryAuthorizationService = {
    //NB: Fail fast here, since on the fully-meshed HMS deployment, all nodes are expected to be
    //query entry points
    require(shrineConfig.queryEntryPointConfig.isDefined, s"${Keys.queryEntryPoint} section must be defined in shrine.conf")

    val queryEntryPointConfig = shrineConfig.queryEntryPointConfig.get

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