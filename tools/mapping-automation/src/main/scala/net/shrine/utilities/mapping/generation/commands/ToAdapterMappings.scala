package net.shrine.utilities.mapping.generation.commands

import net.shrine.utilities.commands.>>>
import net.shrine.ont.OntTerm
import net.shrine.config.mappings.AdapterMappings

/**
 * @author clint
 * @date Jul 17, 2014
 */
object ToAdapterMappings extends (Map[OntTerm, Set[OntTerm]] >>> AdapterMappings) {
  override def apply(terms: Map[OntTerm, Set[OntTerm]]): AdapterMappings = {
    //TODO: FIXME: Get a real value from somewhere
    val version = AdapterMappings.Unknown
    
    val pairs: Seq[(String, String)] = for {
      (shrine, locals) <- terms.toSeq
      local <- locals
    } yield (shrine.toString -> local.toString)
    
    AdapterMappings.empty.withVersion(version) ++ pairs
  }
  
  override def toString = "ToAdapterMappings"
}