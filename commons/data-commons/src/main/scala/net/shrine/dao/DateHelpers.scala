package net.shrine.dao

import javax.xml.datatype.XMLGregorianCalendar
import net.shrine.util.XmlDateHelper
import net.shrine.util.XmlGcEnrichments

/**
 * @author clint
 * @date Oct 16, 2012
 */
object DateHelpers {
  def toTimestamp(xmlGc: XMLGregorianCalendar) = {
    new java.sql.Timestamp(xmlGc.toGregorianCalendar.getTime.getTime)
  }

  def toXmlGc(date: java.sql.Timestamp): XMLGregorianCalendar = {
    XmlDateHelper.toXmlGregorianCalendar(new java.util.Date(date.getTime))
  }

  def daysFromNow(howMany: Int): XMLGregorianCalendar = {
    val now = XmlDateHelper.now
    
    import XmlGcEnrichments._
    import scala.concurrent.duration._
    
    now + howMany.days
  }
}