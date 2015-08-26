package net.shrine.adapter.dao.squeryl.tables

import org.squeryl.Schema
import net.shrine.adapter.dao.model.QueryResultRow
import net.shrine.adapter.dao.model.squeryl.SquerylQueryResultRow
import net.shrine.dao.squeryl.tables.AbstractTableComponent
import net.shrine.dao.squeryl.SquerylEntryPoint

/**
 * @author clint
 * @date May 22, 2013
 */
trait QueryResultsComponent extends AbstractTableComponent { self: Schema with ErrorResultsComponent with CountResultsComponent with BreakdownResultsComponent =>
  import SquerylEntryPoint._

  val queryResults = table[SquerylQueryResultRow]("QUERY_RESULT")
  
  val resultsToCounts = {
    val relation = oneToManyRelation(queryResults, countResults).via {
      (queryResult, countResult) => queryResult.id === countResult.resultId
    }
  
    relation.foreignKeyDeclaration.constrainReference(onDelete.cascade)
    
    relation
  }
  
  val resultsToBreakdowns = {
    val relation = oneToManyRelation(queryResults, breakdownResults).via {
      (queryResult, breakdownResult) => queryResult.id === breakdownResult.resultId
    }
  
    relation.foreignKeyDeclaration.constrainReference(onDelete.cascade)
    
    relation
  }
  
  val resultsToErrors = {
    val relation = oneToManyRelation(queryResults, errorResults).via {
      (queryResult, errorResult) => queryResult.id === errorResult.resultId
    }
  
    relation.foreignKeyDeclaration.constrainReference(onDelete.cascade)
    
    relation
  }

  declareThat(queryResults) {
    _.id is (primaryKey, oracleSafeAutoIncremented("QUERY_RESULT_ID"))
  }
}