package net.shrine.util

import org.junit.Test
import javax.xml.datatype.XMLGregorianCalendar

/**
 * @author clint
 * @date Feb 21, 2014
 */
final class XmlGcEnrichmentsTest extends ShouldMatchersForJUnit {
  import XmlGcEnrichments._ 
  import scala.concurrent.duration._
  
  private def toMillis(xmlGc: XMLGregorianCalendar): Long = xmlGc.toGregorianCalendar.getTimeInMillis
  
  private val now = XmlDateHelper.now
  
  private val plusOneSecond = now + 1.second
  
  private val minusOneSecond = now + (-1).second
  
  @Test
  def testPlus {
    (plusOneSecond.compare(now) > 0) should be(true)
    
    val delta = toMillis(plusOneSecond) - toMillis(now)
    
    delta should equal(1000L)
  }
  
  @Test
  def testPlusNegative {
    (minusOneSecond.compare(now) < 0) should be(true)
    
    val delta = toMillis(minusOneSecond) - toMillis(now)
    
    delta should equal(-1000L)
  }
  
  @Test
  def testComparators {
    plusOneSecond > now should be(true)
    plusOneSecond > minusOneSecond should be(true)
    plusOneSecond > plusOneSecond should be(false)
    
    plusOneSecond >= now should be(true)
    plusOneSecond >= minusOneSecond should be(true)
    plusOneSecond >= plusOneSecond should be(true)
    
    minusOneSecond < now should be(true)
    minusOneSecond < plusOneSecond should be(true)
    minusOneSecond < minusOneSecond should be(false)
    
    minusOneSecond <= now should be(true)
    minusOneSecond <= plusOneSecond should be(true)
    minusOneSecond <= minusOneSecond should be(true)
  }
}