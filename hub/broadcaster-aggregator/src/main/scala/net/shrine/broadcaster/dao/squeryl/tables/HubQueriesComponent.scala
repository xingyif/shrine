package net.shrine.broadcaster.dao.squeryl.tables

import org.squeryl.Schema
import _root_.net.shrine.dao.squeryl.tables.AbstractTableComponent
import _root_.net.shrine.dao.squeryl.SquerylEntryPoint
import _root_.net.shrine.broadcaster.dao.model.squeryl.SquerylHubQueryRow


/**
 * @author clint
 * @date Dec 11, 2014
 */
trait HubQueriesComponent extends AbstractTableComponent { self: Schema with HubQueryResultsComponent =>

  import SquerylEntryPoint._

  val hubQueries = table[SquerylHubQueryRow]("HUB_QUERY")

  declareThat(hubQueries) {
    _.networkQueryId is (primaryKey)
  }
  
  lazy val hubQueriesToResults = {
    val relation = oneToManyRelation(hubQueries, hubQueryResults).via {
      _.networkQueryId === _.networkQueryId
    }
    
    relation.foreignKeyDeclaration.constrainReference(onDelete.cascade)
    
    relation
  }
}