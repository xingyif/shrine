package net.shrine.utilities.mapping.generation.commands

import net.shrine.util.ShouldMatchersForJUnit
import org.junit.Test
import net.shrine.ont.OntTerm
import net.shrine.config.mappings.AdapterMappings

/**
 * @author clint
 * @date Jul 18, 2014
 */
final class ToAdapterMappingsTest extends ShouldMatchersForJUnit {
  @Test
  def testApply: Unit = {
    val xyza = """\\X\Y\Z\A\"""
    val xyz = """\\X\Y\Z\"""
    val xy = """\\X\Y\"""
    val x = """\\X\"""

    val abc = """\\A\B\C\"""
    val abca1 = """\\A\B\C\A1\"""
    val abca2 = """\\A\B\C\A2\"""
    
    val generatedMappings = Map(
      OntTerm(xyza) -> Set(OntTerm(abca1), OntTerm(abca2)),
      OntTerm(xyz) -> Set(OntTerm(abca1), OntTerm(abca2), OntTerm(abc)),
      OntTerm(xy) -> Set(OntTerm(abca1), OntTerm(abca2), OntTerm(abc)),
      OntTerm(x) -> Set(OntTerm(abca1), OntTerm(abca2), OntTerm(abc)))
    
    val adapterMappings = ToAdapterMappings(generatedMappings)
    
    adapterMappings should equal(AdapterMappings(AdapterMappings.Unknown, Map(
      xyza -> Set(abca1, abca2),
      xyz -> Set(abca1, abca2, abc),
      xy -> Set(abca1, abca2, abc),
      x -> Set(abca1, abca2, abc))))
  }
}