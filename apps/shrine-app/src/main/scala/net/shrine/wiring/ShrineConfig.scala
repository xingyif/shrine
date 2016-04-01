package net.shrine.wiring

import com.typesafe.config.Config
import net.shrine.adapter.service.AdapterConfig
import net.shrine.broadcaster.HubConfig
import net.shrine.config.{ConfigExtensions, Keys}
import net.shrine.protocol.{ResultOutputTypes, HiveCredentials, ResultOutputType}
import net.shrine.qep.QepConfig

/**
 * @author clint
 * @since Feb 6, 2013
 */
final case class ShrineConfig(
  adapterConfig: Option[AdapterConfig],
  hubConfig: Option[HubConfig],
  queryEntryPointConfig: Option[QepConfig],
  crcHiveCredentials: HiveCredentials,
  ontHiveCredentials: HiveCredentials,
  adapterStatusQuery: String
                             ) {
  
  //NB: Preparing for the possible case where we'd need distinct credentials for talking to the PM
  def pmHiveCredentials: HiveCredentials = crcHiveCredentials
}

object ShrineConfig {

  def apply(config: Config): ShrineConfig = {

    val configForShrine = config.getConfig("shrine")
    import Keys._

    def getOptionConfiguredIf[T](key:String,constructor: Config => T):Option[T] = {
      if(configForShrine.getBoolean(s"$key.create")) configForShrine.getOptionConfigured(key,constructor)
      else None
    }

    ShrineConfig(
      adapterConfig = getOptionConfiguredIf(adapter, AdapterConfig(_)),
      hubConfig = getOptionConfiguredIf(hub, HubConfig(_)),
      queryEntryPointConfig = getOptionConfiguredIf(queryEntryPoint, QepConfig(_)),
      crcHiveCredentials = configForShrine.getConfigured(hiveCredentials, HiveCredentials(_, HiveCredentials.CRC)),
      ontHiveCredentials = configForShrine.getConfigured(hiveCredentials, HiveCredentials(_, HiveCredentials.ONT)),
      adapterStatusQuery = configForShrine.getString(networkStatusQuery)
    )
  }
}
