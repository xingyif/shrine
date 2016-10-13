package net.shrine.config.mappings

import scala.util.Try

/**
 * @author clint
 * @since Mar 6, 2012
 */

//todo this whole hierarchy of traits is really suspect. Maybe get rid of the whole works, or at least any unused levels.
trait AdapterMappingsSource {
  def load(source:String): Try[AdapterMappings]

  def lastModified: Long
}