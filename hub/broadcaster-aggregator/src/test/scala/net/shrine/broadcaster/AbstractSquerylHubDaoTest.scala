package net.shrine.broadcaster

import net.shrine.dao.squeryl.SquerylEntryPoint
import org.squeryl.Table
import org.squeryl.Query
import net.shrine.broadcaster.dao.model.squeryl.SquerylHubQueryRow
import net.shrine.broadcaster.dao.model.squeryl.SquerylHubQueryResultRow
import net.shrine.broadcaster.dao.model.HubQueryResultRow
import net.shrine.broadcaster.dao.model.HubQueryRow
import net.shrine.dao.squeryl.AbstractSquerylDaoTest
import net.shrine.broadcaster.dao.squeryl.tables.Tables

/**
 * @author clint
 * @date Dec 15, 2014
 */
trait AbstractSquerylHubDaoTest extends AbstractSquerylDaoTest with Wiring {
  override type MyTables = Tables
  
  initializer.init()
  
  protected lazy val queryRows: Query[SquerylHubQueryRow] = allRowsQuery(tables.hubQueries)(identity)

  protected lazy val queryResultRows: Query[SquerylHubQueryResultRow] = allRowsQuery(tables.hubQueryResults)(identity)
}