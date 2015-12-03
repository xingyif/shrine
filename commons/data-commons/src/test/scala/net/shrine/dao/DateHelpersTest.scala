package net.shrine.dao

import org.junit.Test
import net.shrine.util.ShouldMatchersForJUnit
import javax.xml.datatype.XMLGregorianCalendar
import net.shrine.util.XmlDateHelper

/**
 * @author clint
 * @date Nov 1, 2012
 */
final class DateHelpersTest extends ShouldMatchersForJUnit {
  val xmlNow = XmlDateHelper.now
  
  val javaUtilNow = xmlNow.toGregorianCalendar.getTime
    
  val sqlNow = new java.sql.Timestamp(javaUtilNow.getTime)
  
  @Test
  def testToXmlGc {
    val xmlNow = DateHelpers.toXmlGc(sqlNow)
    
    xmlNow.toGregorianCalendar.getTime.getTime should equal(sqlNow.getTime)
  }
  
  @Test
  def testToTimestamp {
    DateHelpers.toTimestamp(xmlNow) should equal(sqlNow)
  }
  
  @Test
  def testTimestampXmlGcRoundTrip {
    val roundTripped = DateHelpers.toXmlGc(DateHelpers.toTimestamp(xmlNow)) 
    
    roundTripped should equal(xmlNow)
    
    def millis(xmlGc: XMLGregorianCalendar) = xmlGc.toGregorianCalendar.getTime.getTime 
    
    millis(roundTripped) should equal(millis(xmlNow)) 
  }
}