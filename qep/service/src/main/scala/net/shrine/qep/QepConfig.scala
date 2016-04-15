package net.shrine.qep

import com.typesafe.config.Config
import net.shrine.authentication.AuthenticationType
import net.shrine.authorization.AuthorizationType
import net.shrine.client.EndpointConfig
import net.shrine.config.{ConfigExtensions, DurationConfigParser, Keys}
import net.shrine.crypto.SigningCertStrategy
import net.shrine.log.Log

import scala.concurrent.duration.Duration

/**
 * @author clint
 * @since Feb 28, 2014
 */
final case class QepConfig (
  authenticationType: AuthenticationType,
  includeAggregateResults: Boolean,
  maxQueryWaitTime: Duration,
  broadcasterServiceEndpoint: Option[EndpointConfig],
  signingCertStrategy: SigningCertStrategy,
  collectQepAudit:Boolean) {

  Log.debug(s"QepConfig collectQepAudit is $collectQepAudit")

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
      config.getBoolean(includeAggregateResults),
      DurationConfigParser(config.getConfig("maxQueryWaitTime")),
      config.getOptionConfigured(broadcasterServiceEndpoint, EndpointConfig(_)),
      signingCertAttachmentStrategy(attachSigningCert,config),
    //todo change to shrine.queryEntryPoint...
      QepConfigSource.config.getBoolean("shrine.queryEntryPoint.audit.collectQepAudit")
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
}
