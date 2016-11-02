package net.shrine.dashboard

import net.shrine.source.ConfigSource

/**
 * Source of typesafe config for the dashboard app.
 *
 * @author david 
 * @since 4/29/15
 */

object DashboardConfigSource extends ConfigSource {
  override val configName = "shrine"
}
