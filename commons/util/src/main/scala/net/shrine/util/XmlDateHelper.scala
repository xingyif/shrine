package net.shrine.util

import javax.xml.datatype.{DatatypeConstants, DatatypeFactory, XMLGregorianCalendar}

import scala.util.Try
import java.util.GregorianCalendar

/**
 * @author clint
 * @since Oct 18, 2012
 */
object XmlDateHelper {

  //NB: Will use current locale
  private[util] lazy val datatypeFactory: DatatypeFactory = DatatypeFactory.newInstance

  //NB: Will use current locale
  def now: XMLGregorianCalendar = withDefinedTimeFields(datatypeFactory.newXMLGregorianCalendar(new GregorianCalendar))

  //NB: Will use current locale
  def parseXmlTime(lexicalRep: String): Try[XMLGregorianCalendar] = Try {
    withDefinedTimeFields(datatypeFactory.newXMLGregorianCalendar(lexicalRep))
  }

  def toXmlGregorianCalendar(date: java.util.Date): XMLGregorianCalendar = {
    val cal = new GregorianCalendar

    cal.setTime(date)

    withDefinedTimeFields(datatypeFactory.newXMLGregorianCalendar(cal))
  }

  def toXmlGregorianCalendar(milliseconds: Long): XMLGregorianCalendar = {
    val cal = new GregorianCalendar

    cal.setTimeInMillis(milliseconds)

    withDefinedTimeFields(datatypeFactory.newXMLGregorianCalendar(cal))
  }

  /**
    * Add fields to ensure that the XMLGregorianCalendar renders as a dateTime with milliseconds and a time zone.
    * i2b2 needs datetime filled in. See SHRINE-2187
    */
  def withDefinedTimeFields(xmlGc:XMLGregorianCalendar): XMLGregorianCalendar = {

    //looks like the only way to get XMLGregorianCalendar to render an xs:datetime
    if(xmlGc.getHour == DatatypeConstants.FIELD_UNDEFINED) xmlGc.setHour(0)
    if(xmlGc.getMinute == DatatypeConstants.FIELD_UNDEFINED) xmlGc.setMinute(0)
    if(xmlGc.getSecond == DatatypeConstants.FIELD_UNDEFINED) xmlGc.setSecond(0)
    if(xmlGc.getMillisecond == DatatypeConstants.FIELD_UNDEFINED) xmlGc.setMillisecond(0)
    if(xmlGc.getTimezone == DatatypeConstants.FIELD_UNDEFINED) xmlGc.setTimezone(0)

    xmlGc
  }

  //todo move out. Has nothing to do with XML
  /**
    * Helper method to trace how long a particular task takes to run.
    *
    */
  def time[T](taskName: String)(log: String => Unit)(f: => T): T = {
    val start = System.currentTimeMillis

    try { f } finally {
      val elapsed = System.currentTimeMillis - start

      log(s"$taskName took $elapsed milliseconds.")
    }
  }
}