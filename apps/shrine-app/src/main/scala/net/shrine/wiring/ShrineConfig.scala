package net.shrine.wiring

import com.typesafe.config.Config
import net.shrine.broadcaster.HubConfig
import net.shrine.config.{ConfigExtensions, Keys}
import net.shrine.protocol.HiveCredentials
import net.shrine.qep.QepConfig

/**
 * @author clint
 * @since Feb 6, 2013
 */
final case class ShrineConfig(
  hubConfig: Option[HubConfig],
  queryEntryPointConfig: Option[QepConfig],
  adapterStatusQuery: String
                             )

object ShrineConfig {

  def apply(config: Config): ShrineConfig = {

    val configForShrine = config.getConfig("shrine")
    import Keys._

    def getOptionConfiguredIf[T](key:String,constructor: Config => T):Option[T] = {
      if(configForShrine.getBoolean(s"$key.create")) configForShrine.getOptionConfigured(key,constructor)
      else None
    }

    ShrineConfig(
      hubConfig = getOptionConfiguredIf(hub, HubConfig(_)),
      queryEntryPointConfig = getOptionConfiguredIf(queryEntryPoint, QepConfig(_)),
      adapterStatusQuery = configForShrine.getString(networkStatusQuery)
    )
  }
}
