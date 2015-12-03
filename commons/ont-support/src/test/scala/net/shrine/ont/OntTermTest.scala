package net.shrine.ont

import net.shrine.util.ShouldMatchersForJUnit
import org.junit.Test

/**
 * @author clint
 * @date Jun 10, 2014
 */
final class OntTermTest extends ShouldMatchersForJUnit {
  @Test
  def testHLevel: Unit = {
    OntTerm("""\\FOO\FOO\BAR\BAZ\""").hLevel should equal(3)
    OntTerm("""\\FOO\FOO\BAR\""").hLevel should equal(2)
    OntTerm("""\\FOO\FOO\""").hLevel should equal(1)
    OntTerm("""\\FOO\""").hLevel should equal(0)
  }
  
  @Test
  def testApplyString: Unit = {
    val term = """\\FOO\FOO\BAR\BAZ\"""
    
    OntTerm(term).parts should equal(Seq("FOO", "FOO", "BAR", "BAZ"))
  }
  
  @Test
  def testToString: Unit = {
    OntTerm(Seq("FOO", "FOO", "BAR", "BAZ")).toString should equal("""\\FOO\FOO\BAR\BAZ\""")
  }
  
  private val emptyTerm = OntTerm(Nil)
  
  @Test
  def testSize: Unit = {
    emptyTerm.size should equal(0)
    
    OntTerm(Seq("FOO", "FOO", "BAR", "BAZ")).size should equal(4)
  }
  
  @Test
  def testStartsWith: Unit = {
    OntTerm(Seq("FOO", "FOO", "BAR", "BAZ")).startsWith(emptyTerm) should be(true)
    
    OntTerm(Seq("FOO", "FOO", "BAR", "BAZ")).startsWith(OntTerm(Seq("FOO"))) should be(true)
    OntTerm(Seq("FOO", "FOO", "BAR", "BAZ")).startsWith(OntTerm(Seq("FOO", "FOO"))) should be(true)
    OntTerm(Seq("FOO", "FOO", "BAR", "BAZ")).startsWith(OntTerm(Seq("FOO", "FOO", "BAR"))) should be(true)
    OntTerm(Seq("FOO", "FOO", "BAR", "BAZ")).startsWith(OntTerm(Seq("FOO", "FOO", "BAR", "BAZ"))) should be(true)
    
    OntTerm(Seq("FOO", "FOO", "BAR", "BAZ")).startsWith(OntTerm(Seq("BLAH", "NUH", "ZUH"))) should be(false)
    emptyTerm.startsWith(OntTerm(Seq("BLAH", "NUH", "ZUH"))) should be(false)
    
    emptyTerm.startsWith(emptyTerm) should be(true)
  }
  
  @Test
  def testEndsWith: Unit = {
    OntTerm(Seq("FOO", "FOO", "BAR", "BAZ")).endsWith(emptyTerm) should be(true)
    
    OntTerm(Seq("FOO", "FOO", "BAR", "BAZ")).endsWith(OntTerm(Seq("BAZ"))) should be(true)
    OntTerm(Seq("FOO", "FOO", "BAR", "BAZ")).endsWith(OntTerm(Seq("BAR", "BAZ"))) should be(true)
    OntTerm(Seq("FOO", "FOO", "BAR", "BAZ")).endsWith(OntTerm(Seq("FOO", "BAR", "BAZ"))) should be(true)
    OntTerm(Seq("FOO", "FOO", "BAR", "BAZ")).endsWith(OntTerm(Seq("FOO", "FOO", "BAR", "BAZ"))) should be(true)
    
    OntTerm(Seq("FOO", "FOO", "BAR", "BAZ")).endsWith(OntTerm(Seq("BLAH", "NUH", "ZUH"))) should be(false)
    emptyTerm.endsWith(OntTerm(Seq("BLAH", "NUH", "ZUH"))) should be(false)
    
    emptyTerm.endsWith(emptyTerm) should be(true)
  }
  
  @Test
  def testParent: Unit = {
    emptyTerm.parent should be(None)
    
    OntTerm(Seq("FOO", "FOO", "BAR", "BAZ")).parent.get should be(OntTerm(Seq("FOO", "FOO", "BAR")))
    OntTerm(Seq("FOO", "FOO", "BAR")).parent.get should be(OntTerm(Seq("FOO", "FOO")))
    OntTerm(Seq("FOO", "FOO")).parent.get should be(OntTerm(Seq("FOO")))
    
    OntTerm(Seq("FOO")).parent should be(None)    
  }
  
  @Test
  def testAncestors: Unit = {
    emptyTerm.ancestors should be(Nil)
    
    OntTerm(Seq("FOO", "FOO", "BAR", "BAZ", "BLERG")).ancestors should be(Seq(OntTerm(Seq("FOO", "FOO", "BAR", "BAZ")), OntTerm(Seq("FOO", "FOO", "BAR")), OntTerm(Seq("FOO", "FOO")), OntTerm(Seq("FOO"))))
    OntTerm(Seq("FOO")).ancestors should be(Nil)    
  }
  
  @Test
  def testIsAncestorAndDescendantOf: Unit = {
    val descendant = OntTerm(Seq("FOO", "FOO", "BAR", "BAZ", "BLERG"))
    
    OntTerm(Seq("FOO", "FOO", "BAR", "BAZ")).isAncestorOf(descendant) should be(true) 
    OntTerm(Seq("FOO", "FOO", "BAR")).isAncestorOf(descendant) should be(true)
    OntTerm(Seq("FOO", "FOO")).isAncestorOf(descendant) should be(true)
    OntTerm(Seq("FOO")).isAncestorOf(descendant) should be(true)
    
    descendant.isDescendantOf(OntTerm(Seq("FOO", "FOO", "BAR", "BAZ"))) should be(true) 
    descendant.isDescendantOf(OntTerm(Seq("FOO", "FOO", "BAR"))) should be(true)
    descendant.isDescendantOf(OntTerm(Seq("FOO", "FOO"))) should be(true)
    descendant.isDescendantOf(OntTerm(Seq("FOO"))) should be(true)
    
    val xyz = OntTerm("""\\X\Y\Z""")
    
    xyz.isAncestorOf(descendant) should be(false)
    descendant.isAncestorOf(xyz) should be(false)
    
    descendant.isDescendantOf(xyz) should be(false)
    xyz.isDescendantOf(descendant) should be(false)
    
    xyz.isAncestorOf(xyz) should be(false)
    xyz.isDescendantOf(xyz) should be(false)
    
    val xya = OntTerm("""\\X\Y\A""")
    
    xyz.isAncestorOf(xya) should be(false)
    xya.isDescendantOf(xyz) should be(false)
  }
  
  @Test
  def testIsRelativeOf: Unit = {
    val descendant = OntTerm(Seq("FOO", "FOO", "BAR", "BAZ", "BLERG"))
    
    OntTerm(Seq("FOO", "FOO", "BAR", "BAZ")).isRelativeOf(descendant) should be(true) 
    OntTerm(Seq("FOO", "FOO", "BAR")).isRelativeOf(descendant) should be(true)
    OntTerm(Seq("FOO", "FOO")).isRelativeOf(descendant) should be(true)
    OntTerm(Seq("FOO")).isRelativeOf(descendant) should be(true)
    
    descendant.isRelativeOf(OntTerm(Seq("FOO", "FOO", "BAR", "BAZ"))) should be(true) 
    descendant.isRelativeOf(OntTerm(Seq("FOO", "FOO", "BAR"))) should be(true)
    descendant.isRelativeOf(OntTerm(Seq("FOO", "FOO"))) should be(true)
    descendant.isRelativeOf(OntTerm(Seq("FOO"))) should be(true)
    
    val xyz = OntTerm("""\\X\Y\Z""")
    
    val abc = OntTerm("""\\A\B\C""")
    
    xyz.isRelativeOf(xyz) should be(true)
    
    xyz.isRelativeOf(abc) should be(false)
    abc.isRelativeOf(xyz) should be(false)
  }
}