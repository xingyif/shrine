package net.shrine.adapter.dao.squeryl.tables

import org.squeryl.Schema
import net.shrine.adapter.dao.model.ShrineQuery
import net.shrine.adapter.dao.model.squeryl.SquerylShrineQuery
import net.shrine.dao.squeryl.tables.AbstractTableComponent
import net.shrine.dao.squeryl.SquerylEntryPoint

/**
 * @author clint
 * @date May 22, 2013
 */
trait ShrineQueriesComponent extends AbstractTableComponent { self: Schema with QueryResultsComponent =>

  import SquerylEntryPoint._

  val shrineQueries = table[SquerylShrineQuery]("SHRINE_QUERY")

  val queriesToResults = {
    val relation = oneToManyRelation(shrineQueries, queryResults).via {
      (query, result) => query.id === result.queryId
    }
    
    relation.foreignKeyDeclaration.constrainReference(onDelete.cascade)
    
    relation
  }
  
  declareThat(shrineQueries)(
    _.id is (primaryKey, oracleSafeAutoIncremented("SHRINE_QUERY_ID"))
  )
}