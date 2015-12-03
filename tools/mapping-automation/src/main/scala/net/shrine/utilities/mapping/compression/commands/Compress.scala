package net.shrine.utilities.mapping.compression.commands

import net.shrine.utilities.commands.>>>
import net.shrine.ont.OntTerm
import net.shrine.utilities.mapping.compression.Compressor
import net.shrine.config.mappings.AdapterMappings

/**
 * @author clint
 * @date Aug 1, 2014
 */
object Compress extends (Map[OntTerm, Set[OntTerm]] >>> Map[OntTerm, Set[OntTerm]]) {
  override def apply(mappings: Map[OntTerm, Set[OntTerm]]): Map[OntTerm, Set[OntTerm]] = {
    Compressor.compress(mappings)
  } 
}