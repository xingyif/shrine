package net.shrine.dao.squeryl.tables

import org.squeryl.dsl.ast.BaseColumnAttributeAssignment
import org.squeryl.Table
import org.squeryl.Schema
import org.squeryl.internals.AutoIncremented

/**
 * @author clint
 * @date May 22, 2013
 */
trait AbstractTableComponent { self: Schema =>
  protected def declareThat[E](table: Table[E])(statements: (E => BaseColumnAttributeAssignment)*) {
    on(table) { entity =>
      statements.map(statement => statement(entity))
    }
  }
  
  protected def oracleSafeAutoIncremented(sequenceName: String): AutoIncremented = {
    require(sequenceName != null)
    require(sequenceName.size <= 30) //Oracle will blow up on identifiers > 30 chars 
    
    autoIncremented(sequenceName)
  }
}