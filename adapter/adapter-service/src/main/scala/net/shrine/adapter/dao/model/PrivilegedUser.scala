package net.shrine.adapter.dao.model

import javax.xml.datatype.XMLGregorianCalendar
import net.shrine.util.XmlDateHelper

/**
 * @author clint
 * @date Oct 16, 2012
 * 
 * NB: Can't be final, since Squeryl runs this class through cglib to make a synthetic subclass :(
 */
final case class PrivilegedUser(
  val id: Int,
  val username: String,
  val domain: String,
  val threshold: Int,
  val overrideDate: Option[XMLGregorianCalendar])