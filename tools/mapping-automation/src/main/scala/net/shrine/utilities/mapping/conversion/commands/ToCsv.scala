package net.shrine.utilities.mapping.conversion.commands

import net.shrine.utilities.commands.>>>
import net.shrine.config.mappings.AdapterMappings

/**
 * @author clint
 * @date Jul 16, 2014
 */
object ToCsv extends (AdapterMappings >>> String) {
  override def apply(mappings: AdapterMappings): String = mappings.toCsv
  
  override def toString = "ToCsv"
}