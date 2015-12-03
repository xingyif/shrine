package net.shrine.adapter

import net.shrine.util.ShouldMatchersForJUnit
import org.junit.Test
import scala.collection.mutable.Buffer
import scala.collection.mutable.ArrayBuffer

/**
 * @author clint
 * @date Dec 18, 2013
 */
final class WeakSetTest extends ShouldMatchersForJUnit {
  private val s1 = "foo"
  private val s2 = "bar"
  
  @Test
  def testColonPlus {
    val empty = WeakSet.empty[String]
    
    val weakSet = empty :+ s1 :+ s2
    
    weakSet.values should equal(Seq(s1, s2))
  }
  
  @Test
  def testForeach {
    val weakSet = WeakSet.empty :+ s1 :+ s2
    
    val buffer: Buffer[String] = new ArrayBuffer
    
    weakSet.foreach(buffer.+=)
    
    buffer.toSet should equal(Set(s1, s2))
  }
  
  @Test
  def testEmptyIsEmptyValues {
    val e = WeakSet.empty[String]
    
    e.isEmpty should be(true)
    e.values.isEmpty should be(true)
    
    val weakSet = e :+ s1
    
    e.isEmpty should be(true)
    e.values.isEmpty should be(true)
    
    weakSet.isEmpty should be(false)
    weakSet.values should equal(Seq(s1))
    
    val weakSet2 = weakSet :+ s2
    
    e.isEmpty should be(true)
    e.values.isEmpty should be(true)
    
    weakSet.isEmpty should be(false)
    weakSet.values should equal(Seq(s1))
    
    weakSet2.isEmpty should be(false)
    weakSet2.values should equal(Seq(s1, s2))
  }
}