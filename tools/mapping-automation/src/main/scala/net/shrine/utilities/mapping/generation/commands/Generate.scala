package net.shrine.utilities.mapping.generation.commands

import net.shrine.utilities.commands.>>>
import net.shrine.ont.OntTerm
import net.shrine.ont.MappingGenerator

/**
 * @author clint
 * @date Jul 17, 2014
 */
final case class Generate(hLevelToStopAt: Option[Int]) extends (Iterator[(String, String)] >>> Map[OntTerm, Set[OntTerm]]) {
  override def apply(lines: Iterator[(String, String)]): Map[OntTerm, Set[OntTerm]] = {
    
    val asOntTerms = lines.map { case (shrine, i2b2) => (OntTerm(shrine), OntTerm(i2b2)) }
    
    MappingGenerator.generate(asOntTerms, hLevelToStopAt)
  }
  
  override def toString = "Generate"
}