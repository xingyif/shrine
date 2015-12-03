package net.shrine.utilities.mapping.generation.commands

import net.shrine.util.ShouldMatchersForJUnit
import org.junit.Test
import net.shrine.config.mappings.AdapterMappings
import net.shrine.ont.OntTerm

/**
 * @author clint
 * @date Jul 18, 2014
 */
final class GenerateTest extends ShouldMatchersForJUnit {
  @Test
  def testApply: Unit = {
    val xyza = """\\X\Y\Z\A\"""
    val xyz = """\\X\Y\Z\"""
    val xy = """\\X\Y\"""
    val x = """\\X\"""

    val abc = """\\A\B\C\"""
    val abca1 = """\\A\B\C\A1\"""
    val abca2 = """\\A\B\C\A2\"""

    val simpleMappingPairs = Seq(
      xyza -> abca1,
      xyza -> abca2,
      xyz -> abc)

    {
      val generated = Generate(None)(simpleMappingPairs.iterator)

      generated should equal(Map(
        OntTerm(xyza) -> Set(OntTerm(abca1), OntTerm(abca2)),
        OntTerm(xyz) -> Set(OntTerm(abca1), OntTerm(abca2), OntTerm(abc)),
        OntTerm(xy) -> Set(OntTerm(abca1), OntTerm(abca2), OntTerm(abc)),
        OntTerm(x) -> Set(OntTerm(abca1), OntTerm(abca2), OntTerm(abc))))
    }
    
    {
      val generated = Generate(Some(1))(simpleMappingPairs.iterator)

      generated should equal(Map(
        OntTerm(xyza) -> Set(OntTerm(abca1), OntTerm(abca2)),
        OntTerm(xyz) -> Set(OntTerm(abca1), OntTerm(abca2), OntTerm(abc)),
        OntTerm(xy) -> Set(OntTerm(abca1), OntTerm(abca2), OntTerm(abc))))
    }
  }
}