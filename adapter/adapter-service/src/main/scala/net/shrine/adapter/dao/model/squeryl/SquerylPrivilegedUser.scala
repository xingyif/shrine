package net.shrine.adapter.dao.model.squeryl

import java.sql.Timestamp
import net.shrine.util.XmlDateHelper
import org.squeryl.annotations.Column
import net.shrine.dao.DateHelpers
import javax.xml.datatype.XMLGregorianCalendar
import net.shrine.adapter.dao.model.PrivilegedUser
import org.squeryl.KeyedEntity

/**
 * @author clint
 * @since May 28, 2013
 */
case class SquerylPrivilegedUser(
    @Column(name = "ID")
    id: Int,
    @Column(name = "USERNAME")
    username: String,
    @Column(name = "DOMAIN")
    domain: String,
    @Column(name = "THRESHOLD")
    threshold: Option[Int],
    @Column(name = "OVERRIDE_DATE")
    overrideDate: Option[Timestamp]) extends KeyedEntity[Int] {
  
  def this(
      id: Int,
      username: String,
      domain: String,
      threshold: Option[Int],
      overrideDate: Option[XMLGregorianCalendar])(implicit dummy: Int = 42) = this(id, username, domain, threshold, overrideDate.map(DateHelpers.toTimestamp))
  
  //NB: For Squeryl, ugh :(
  def this() = this(0, "", "", None, Option(XmlDateHelper.now))
  
  def toPrivilegedUser = PrivilegedUser(id, username, domain, threshold, overrideDate.map(DateHelpers.toXmlGc))
}
