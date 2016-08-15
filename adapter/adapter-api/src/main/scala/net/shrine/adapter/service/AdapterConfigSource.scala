package net.shrine.adapter.service

import net.shrine.source.ConfigSource

/**
 * Source of config for the Adapter. Put new config fields here, not in AdapterConfig, to enable Config-based apply() methods.
 *
 * @author david
 * @since 8/25/15
 */
object AdapterConfigSource extends ConfigSource {

  override val configName = "shrine"
}