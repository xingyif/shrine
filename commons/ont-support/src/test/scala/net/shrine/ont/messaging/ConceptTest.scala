package net.shrine.ont.messaging

import org.junit.Test
import net.shrine.ont.OntTerm

/**
 * @author Dave Ortiz (?)
 * @author Clint Gilbert
 * 
 * @date Feb 8, 2012
 */
@Test
final class ConceptTest extends FromJsonTest(Concept) {
  val cat1 = "category1"
  val cat2 = "category2"
  
  import OntTerm.shrinePrefix
  
  val concept1 = Concept(shrinePrefix + cat1 + """\concept1Key\""", Some("someSynonym"))
  val concept2 = Concept(shrinePrefix + cat1 + """\concept2Key""", None)
  val concept3 = Concept(shrinePrefix + cat2 + """\concept3Key""", Some("someSynonym"))

  @Test
  def testToJsonString {
    concept1.toJsonString() should equal("""{"path":"\\\\SHRINE\\SHRINE\\category1\\concept1Key\\","synonym":"someSynonym","category":"category1","simpleName":"concept1Key"}""")
    
    concept2.toJsonString() should equal("""{"path":"\\\\SHRINE\\SHRINE\\category1\\concept2Key","category":"category1","simpleName":"concept2Key"}""")
  }
  
  @Test
  def testCategory {
    concept1.category should equal(cat1)
    concept2.category should equal(cat1)
    concept3.category should equal(cat2)
  }

  @Test
  def testSimpleName {
    concept1.simpleName should equal("concept1Key")
    concept2.simpleName should equal("concept2Key")
    concept3.simpleName should equal("concept3Key")
  }
  
  @Test
  def testFromJson = {
    doTestFromJson(concept1)
    doTestFromJson(concept2)
  }
}