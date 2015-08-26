package net.shrine.config.mappings

import scala.util.Try

/**
 * @author clint
 * @date Mar 6, 2012
 */
trait AdapterMappingsSource {
  def load: Try[AdapterMappings] 
}