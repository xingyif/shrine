package net.shrine.broadcaster.dao.squeryl.tables

import org.squeryl.Schema
import _root_.net.shrine.dao.squeryl.tables.AbstractTableComponent
import _root_.net.shrine.dao.squeryl.SquerylEntryPoint
import _root_.net.shrine.broadcaster.dao.model.squeryl.SquerylHubQueryRow
import _root_.net.shrine.broadcaster.dao.model.squeryl.SquerylHubQueryResultRow
import org.squeryl.dsl.CompositeKey2


/**
 * @author clint
 * @date Dec 11, 2014
 */
trait HubQueryResultsComponent extends AbstractTableComponent { self: Schema with HubQueriesComponent =>

  import SquerylEntryPoint._

  val hubQueryResults = table[SquerylHubQueryResultRow]("HUB_QUERY_RESULT")

  declareThat(hubQueryResults) { queryResult =>
    queryResult.id is (primaryKey, oracleSafeAutoIncremented("QUERY_RESULT_ID"))
    queryResult.networkQueryId is(indexed)
    queryResult.nodeName is(indexed)
  }
}