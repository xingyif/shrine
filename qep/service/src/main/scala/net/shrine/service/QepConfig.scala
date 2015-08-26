package net.shrine.service

import com.typesafe.config.Config
import net.shrine.authentication.AuthenticationType
import net.shrine.authorization.AuthorizationType
import net.shrine.authorization.steward.StewardConfig
import net.shrine.client.EndpointConfig
import net.shrine.config.{DurationConfigParser, Keys,ConfigExtensions}
import net.shrine.crypto.SigningCertStrategy
import net.shrine.protocol.CredentialConfig

import scala.concurrent.duration.Duration

/**
 * @author clint
 * @since Feb 28, 2014
 */
final case class QepConfig(
  authenticationType: AuthenticationType,
  authorizationType: AuthorizationType,
  //NB: optional, only needed for HMS
  sheriffEndpoint: Option[EndpointConfig],
  //NB: optional, only needed for HMS
  sheriffCredentials: Option[CredentialConfig],
  //steward config, only needed for a data steward
  stewardConfig: Option[StewardConfig],
  includeAggregateResults: Boolean,
  maxQueryWaitTime: Duration,
  broadcasterServiceEndpoint: Option[EndpointConfig],
  signingCertStrategy: SigningCertStrategy,
  collectQepAudit:Boolean) {
  
  def broadcasterIsLocal: Boolean = broadcasterServiceEndpoint.isEmpty 
}

object QepConfig {

  val defaultAuthenticationType: AuthenticationType = AuthenticationType.Pm
  
  val defaultAuthorizationType: AuthorizationType = AuthorizationType.NoAuthorization
  
  def apply(config: Config): QepConfig = {
    import Keys._

    QepConfig(
    //todo put these default values in reference.conf if you decide to use one
      optionalAuthenticationType(authenticationType,config).getOrElse(defaultAuthenticationType),
      optionalAuthorizationType(authorizationType,config).getOrElse(defaultAuthorizationType),
      endpointOption(sheriffEndpoint,config),
      credentialsOption(sheriffCredentials,config),
      stewardOption(shrineSteward,config),
      config.getBoolean(includeAggregateResults),
      DurationConfigParser(config.getConfig(maxQueryWaitTime)),
      endpointOption(broadcasterServiceEndpoint,config),
      signingCertAttachmentStrategy(attachSigningCert,config),
      QepConfigSource.config.getBoolean("shrine.qep.audit.collectQepAudit")
    )
  }

  def optionalAuthorizationType(k: String,config: Config): Option[AuthorizationType] = {
    config.getOption(k,_.getString).flatMap(AuthorizationType.valueOf)
  }

  def optionalAuthenticationType(k: String,config: Config): Option[AuthenticationType] = {
    config.getOption(k,_.getString).flatMap(AuthenticationType.valueOf)
  }

  def signingCertAttachmentStrategy(k: String,config: Config): SigningCertStrategy = {
    val attachSigningCerts: Boolean = config.getOption(k, _.getBoolean).getOrElse(false)

    import SigningCertStrategy._

    if(attachSigningCerts) Attach else DontAttach
  }

  def stewardOption(k: String,config: Config): Option[StewardConfig] = config.getOptionConfigured(k, StewardConfig(_))

  def credentialsOption(k: String,config: Config):Option[CredentialConfig] = config.getOptionConfigured(k, CredentialConfig(_))

  def endpointOption(k: String,config: Config): Option[EndpointConfig] = config.getOptionConfigured(k, EndpointConfig(_))
}
