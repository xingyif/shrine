package net.shrine.adapter.service

import com.typesafe.config.Config
import net.shrine.client.EndpointConfig
import net.shrine.config.{DurationConfigParser, ConfigExtensions, Keys}

import scala.concurrent.duration.Duration

/**
 * @author clint
 * @since Jan 17, 2014
 */
final case class AdapterConfig(
  crcEndpoint: EndpointConfig,
  setSizeObfuscation: Boolean,
  adapterLockoutAttemptsThreshold: Int,
  adapterMappingsFileName: String,
  maxSignatureAge: Duration,
  immediatelyRunIncomingQueries: Boolean,
  collectAdapterAudit:Boolean
)

object AdapterConfig {

  val defaultImmediatelyRunIncomingQueries = true
  
  def apply(config: Config): AdapterConfig = {

    import Keys._
    AdapterConfig(
      config.getConfigured(crcEndpoint,EndpointConfig(_)),
      config.getBoolean(setSizeObfuscation),
      config.getInt(adapterLockoutAttemptsThreshold),
      config.getString(adapterMappingsFileName),
      config.getConfigured(maxSignatureAge,DurationConfigParser(_)),
    //todo put a default in the reference.conf
      config.getOption(immediatelyRunIncomingQueries, _.getBoolean).getOrElse(defaultImmediatelyRunIncomingQueries),
      AdapterConfigSource.config.getBoolean("shrine.adapter2.audit.collectAdapterAudit")
    )
  }
}