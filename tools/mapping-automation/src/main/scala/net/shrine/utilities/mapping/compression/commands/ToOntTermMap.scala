package net.shrine.utilities.mapping.compression.commands

import net.shrine.utilities.commands.>>>
import net.shrine.ont.OntTerm
import net.shrine.config.mappings.AdapterMappings

/**
 * @author clint
 * @date Aug 1, 2014
 */
object ToOntTermMap extends (Iterator[(String, String)] >>> Map[OntTerm, Set[OntTerm]]) {
  override def apply(termPairs: Iterator[(String, String)]): Map[OntTerm, Set[OntTerm]] = {
    val adapterMappings = AdapterMappings.empty ++ termPairs
    
    adapterMappings.mappings.map { case (shrine, i2b2Terms) => (OntTerm(shrine), i2b2Terms.map(OntTerm(_))) }
  }
}