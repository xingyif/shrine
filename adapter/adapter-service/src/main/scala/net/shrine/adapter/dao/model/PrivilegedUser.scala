package net.shrine.adapter.dao.model

import javax.xml.datatype.XMLGregorianCalendar

/**
 * @author clint
 * @since Oct 16, 2012
 * 
 * NB: Can't be final, since Squeryl runs this class through cglib to make a synthetic subclass :(
 */
final case class PrivilegedUser(
  id: Int,
  username: String,
  domain: String,
  threshold: Option[Int],
  overrideDate: Option[XMLGregorianCalendar])