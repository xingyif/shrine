package net.shrine.adapter.dao.squeryl.tables

import org.squeryl.Schema
import net.shrine.adapter.dao.model.squeryl.SquerylShrineError
import net.shrine.dao.squeryl.tables.AbstractTableComponent
import net.shrine.dao.squeryl.SquerylEntryPoint

/**
 * @author clint
 * @date May 22, 2013
 */
trait ErrorResultsComponent extends AbstractTableComponent { self: Schema =>
  import SquerylEntryPoint._
  
  val errorResults = table[SquerylShrineError]("ERROR_RESULT")
  
  declareThat(errorResults) { 
    _.id is (primaryKey, oracleSafeAutoIncremented("ERROR_RESULT_ID"))
  }
}