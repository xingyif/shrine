package net.shrine.wiring

import com.typesafe.config.Config
import net.shrine.adapter.service.AdapterConfig
import net.shrine.broadcaster.HubConfig
import net.shrine.client.EndpointConfig
import net.shrine.config.{ConfigExtensions, Keys}
import net.shrine.crypto.{KeyStoreDescriptorParser, KeyStoreDescriptor}
import net.shrine.protocol.{ResultOutputTypes, HiveCredentials, ResultOutputType}
import net.shrine.service.QepConfig

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
  //TODO: Do we want seperate hive credentials for talking to the Ont cell?
  //On the dev stack, the project id is the only thing that needs to change, but can we rely on that?
  pmEndpoint: EndpointConfig,
  ontEndpoint: EndpointConfig,
  adapterStatusQuery: String,
  humanReadableNodeName: String,
  shrineDatabaseType: String,
  keystoreDescriptor: KeyStoreDescriptor,
  breakdownResultOutputTypes: Set[ResultOutputType]) {
  
  //NB: Preparing for the possible case where we'd need distinct credentials for talking to the PM
  def pmHiveCredentials: HiveCredentials = crcHiveCredentials
}

object ShrineConfig {

  def apply(config: Config): ShrineConfig = {

    val configForShrine = config.getConfig("shrine")
    import Keys._

    ShrineConfig(
      configForShrine.getOptionConfigured(adapter, AdapterConfig(_)),
      configForShrine.getOptionConfigured(hub, HubConfig(_)),
      configForShrine.getOptionConfigured(queryEntryPoint, QepConfig(_)),
      configForShrine.getConfigured(hiveCredentials,HiveCredentials(_,HiveCredentials.CRC)),
      configForShrine.getConfigured(hiveCredentials,HiveCredentials(_,HiveCredentials.ONT)),
      configForShrine.getConfigured(pmEndpoint,EndpointConfig(_)),
      configForShrine.getConfigured(ontEndpoint,EndpointConfig(_)),
      configForShrine.getString(networkStatusQuery),
      configForShrine.getString(humanReadableNodeName),
      configForShrine.getString(shrineDatabaseType),
      configForShrine.getConfigured(keystore,KeyStoreDescriptorParser(_)),
      configForShrine.getOptionConfigured(breakdownResultOutputTypes,ResultOutputTypes.fromConfig).getOrElse(Set.empty)
    )
  }
}
