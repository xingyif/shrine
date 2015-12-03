package net.shrine.adapter.dao.model.squeryl

import net.shrine.adapter.dao.model.ResultRow
import net.shrine.adapter.dao.model.BreakdownResultRow
import org.squeryl.KeyedEntity
import org.squeryl.annotations.Column

/**
 * @author clint
 * @date May 28, 2013
 */
case class SquerylBreakdownResultRow(
  @Column(name = "ID")
  id: Int,
  @Column(name = "RESULT_ID")
  resultId: Int,
  @Column(name = "DATA_KEY")
  dataKey: String,
  @Column(name = "ORIGINAL_VALUE")
  originalValue: Long,
  @Column(name = "OBFUSCATED_VALUE")
  obfuscatedValue: Long) extends KeyedEntity[Int] {
  
  //NB: For Squeryl, ugh :(
  def this() = this(0, 0, "", 0L, 0L)
  
  def toBreakdownResultRow = BreakdownResultRow(id, resultId, dataKey, originalValue, obfuscatedValue)
}
