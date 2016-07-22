package net.shrine.qep

import net.shrine.config.ConfigSource
import net.shrine.log.Log

/**
 * Source of config for the Qep. Put new config fields here, not in QepConfig, to enable Config-based apply() methods.
 *
 * @author david 
 * @since 8/18/15
 */
object QepConfigSource extends ConfigSource {

  override val configName = "shrine"

  Log.debug(s"shrine.queryEntryPoint.audit.collectQepAudit is ${config.getBoolean("shrine.queryEntryPoint.audit.collectQepAudit")}")

}