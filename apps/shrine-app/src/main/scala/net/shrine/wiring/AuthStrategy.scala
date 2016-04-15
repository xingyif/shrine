package net.shrine.wiring

import com.typesafe.config.Config
import net.shrine.config.ConfigExtensions
import net.shrine.authentication.{AuthenticationType, Authenticator, PmAuthenticator}
import net.shrine.authorization.{AllowsAllAuthorizationService, AuthorizationType, QueryAuthorizationService, StewardQueryAuthorizationService}
import net.shrine.client.Poster
import net.shrine.hms.authentication.EcommonsPmAuthenticator
import net.shrine.hms.authorization.HmsDataStewardAuthorizationService
import net.shrine.qep.AllowsAllAuthenticator

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
    case _ => throw new IllegalArgumentException(s"Unknown authentication type '$authType'")
  }

  def determineQueryAuthorizationService(qepConfig:Config, authenticator: Authenticator): QueryAuthorizationService = {

    val defaultAuthorizationType: AuthorizationType = AuthorizationType.NoAuthorization //todo should default to DSA in the reference.conf instead of being optional
    val authorizationType = qepConfig.getOption("authorizationType",_.getString).flatMap(AuthorizationType.valueOf).getOrElse(defaultAuthorizationType)

    authorizationType match {
      case ShrineSteward => StewardQueryAuthorizationService(qepConfig.getConfig("shrineSteward"))
      case HmsSteward => HmsDataStewardAuthorizationService(qepConfig,authenticator)
      case NoAuthorization => AllowsAllAuthorizationService
      case _ => throw new IllegalArgumentException(s"Unknown authorization type '$authorizationType'")
    }
  }
}