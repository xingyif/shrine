package net.shrine.adapter.dao.model.squeryl

import java.sql.Timestamp
import net.shrine.adapter.dao.model.HasResultId
import java.util.GregorianCalendar
import java.util.Calendar
import net.shrine.adapter.dao.model.CountRow
import net.shrine.dao.DateHelpers
import javax.xml.datatype.XMLGregorianCalendar
import net.shrine.util.XmlDateHelper
import org.squeryl.KeyedEntity
import org.squeryl.annotations.Column

/**
 * @author clint
 * @date May 28, 2013
 */
case class SquerylCountRow(
    @Column(name = "ID")
    id: Int,
    @Column(name = "RESULT_ID")
    resultId: Int,
    @Column(name = "ORIGINAL_COUNT")
    originalValue: Long,
    @Column(name = "OBFUSCATED_COUNT")
    obfuscatedValue: Long,
    @Column(name = "DATE_CREATED")
    creationDate: Timestamp) extends KeyedEntity[Int] {
  
  def this(
      id: Int,
      resultId: Int,
      originalValue: Long, 
      obfuscatedValue: Long, 
      creationDate: XMLGregorianCalendar) = this(id, resultId, originalValue, obfuscatedValue, DateHelpers.toTimestamp(creationDate))
  
  //NB: For Squeryl, ugh :(
  def this() = this(0, 0, 0L, 0L, XmlDateHelper.now)
  
  def toCountRow: CountRow = CountRow(id, resultId, originalValue, obfuscatedValue, DateHelpers.toXmlGc(creationDate))
}