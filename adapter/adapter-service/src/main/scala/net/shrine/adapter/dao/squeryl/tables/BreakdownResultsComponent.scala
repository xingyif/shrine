package net.shrine.adapter.dao.squeryl.tables

import org.squeryl.Schema
import net.shrine.adapter.dao.model.BreakdownResultRow
import net.shrine.adapter.dao.model.squeryl.SquerylBreakdownResultRow
import net.shrine.dao.squeryl.tables.AbstractTableComponent
import net.shrine.dao.squeryl.SquerylEntryPoint

/**
 * @author clint
 * @date May 22, 2013
 */
trait BreakdownResultsComponent extends AbstractTableComponent { self: Schema =>
  import SquerylEntryPoint._
  
  val breakdownResults = table[SquerylBreakdownResultRow]("BREAKDOWN_RESULT")
  
  declareThat(breakdownResults) { 
    _.id is (primaryKey, oracleSafeAutoIncremented("BREAKDOWN_RESULT_ID"))
  }
}