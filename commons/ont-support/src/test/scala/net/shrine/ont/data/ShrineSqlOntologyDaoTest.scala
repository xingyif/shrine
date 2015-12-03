package net.shrine.ont.data

import net.shrine.util.ShouldMatchersForJUnit
import org.scalatest.junit.AssertionsForJUnit
import org.junit.Test
import net.shrine.ont.messaging.Concept

/**
 * @author Clint Gilbert
 * 
 * @date Feb 8, 2012
 */
@Test
final class ShrineSqlOntologyDaoTest extends ShouldMatchersForJUnit with HasShrineSqlStream {
  @Test
  def testGuards {
    intercept[IllegalArgumentException] {
      new ShrineSqlOntologyDao(null)
    }
  }
  
  @Test
  def testLoadAllEntries {
    val concepts = (new ShrineSqlOntologyDao(shrineSqlStream)).ontologyEntries
    
    concepts.size should equal(660) //magic, number of entries in src/test/resources/ShrineWithSyns.sql
    
    val fordyceConcepts = concepts.filter(c => c.path.toLowerCase.contains("fordyce") || c.synonym.exists(_.toLowerCase.contains("fordyce"))).toList
    
    val expected = Seq(Concept("""\\SHRINE\SHRINE\Diagnoses\Diseases of the skin and subcutaneous tissue\Other skin disorders\Fox-Fordyce disease\""", Some("Fox-Fordyce disease"), Some("705.82")),
    				   Concept("""\\SHRINE\SHRINE\Diagnoses\Diseases of the skin and subcutaneous tissue\Other skin disorders\Fox-Fordyce disease\""", Some("Fox-Fordyce disease"), Some("705.82")),
    				   Concept("""\\SHRINE\SHRINE\Diagnoses\Diseases of the skin and subcutaneous tissue\Other skin disorders\Fox-Fordyce disease\""", Some("Fordyce-Fox disease"), Some("705.82")),
    				   Concept("""\\SHRINE\SHRINE\Diagnoses\Diseases of the skin and subcutaneous tissue\Other skin disorders\Fox-Fordyce disease\""", Some("Apocrine miliaria"), Some("705.82")))
    				   
    val Seq(firstExpected, secondExpected, thirdExpected, fourthExpected) = expected
    
    val Seq(firstActual, secondActual, thirdActual, fourthActual) = expected
    				 
    firstActual should equal(firstExpected)
    secondActual should equal(secondExpected)
    thirdActual should equal(thirdExpected)
    fourthActual should equal(fourthExpected)
    
    fordyceConcepts should equal(expected)
  }
}