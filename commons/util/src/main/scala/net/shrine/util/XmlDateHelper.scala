package net.shrine.util

import javax.xml.datatype.XMLGregorianCalendar
import scala.util.Try
import javax.xml.datatype.DatatypeFactory
import java.util.GregorianCalendar

/**
 * @author clint
 * @since Oct 18, 2012
 */
object XmlDateHelper {

  //NB: Will use current locale
  private[util] lazy val datatypeFactory: DatatypeFactory = DatatypeFactory.newInstance

  //NB: Will use current locale
  def now: XMLGregorianCalendar = datatypeFactory.newXMLGregorianCalendar(new GregorianCalendar)

  //NB: Will use current locale
  def parseXmlTime(lexicalRep: String): Try[XMLGregorianCalendar] = Try {
    datatypeFactory.newXMLGregorianCalendar(lexicalRep)
  }

  def toXmlGregorianCalendar(date: java.util.Date): XMLGregorianCalendar = {
    val cal = new GregorianCalendar

    cal.setTime(date)

    datatypeFactory.newXMLGregorianCalendar(cal)
  }

  def toXmlGregorianCalendar(milliseconds: Long): XMLGregorianCalendar = {
    val cal = new GregorianCalendar

    cal.setTimeInMillis(milliseconds)

    datatypeFactory.newXMLGregorianCalendar(cal)
  }

  //todo move out. Has nothing to do with XML
  def time[T](taskName: String)(log: String => Unit)(f: => T): T = {
    val start = System.currentTimeMillis

    try { f } finally {
      val elapsed = System.currentTimeMillis - start

      log(s"$taskName took $elapsed milliseconds.")
    }
  }
}