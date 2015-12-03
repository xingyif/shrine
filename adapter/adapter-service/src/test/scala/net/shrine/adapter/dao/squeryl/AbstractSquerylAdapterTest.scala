package net.shrine.adapter.dao.squeryl

import javax.annotation.Resource
import net.shrine.adapter.dao.AdapterDao
import net.shrine.adapter.dao.squeryl.tables.Tables
import org.squeryl.Table
import org.squeryl.Query
import net.shrine.adapter.dao.model.BreakdownResultRow
import net.shrine.adapter.dao.model.ShrineError
import net.shrine.adapter.dao.model.CountRow
import net.shrine.adapter.dao.model.QueryResultRow
import net.shrine.adapter.dao.model.ShrineQuery
import net.shrine.dao.squeryl.SquerylInitializer
import net.shrine.adapter.Wiring
import net.shrine.dao.squeryl.SquerylEntryPoint
import net.shrine.protocol.DefaultBreakdownResultOutputTypes
import net.shrine.dao.squeryl.AbstractSquerylDaoTest

/**
 * @author clint
 * @date May 24, 2013
 */
trait AbstractSquerylAdapterTest extends AbstractSquerylDaoTest with Wiring {
  
  override type MyTables = Tables
  
  protected lazy val queryRows: Query[ShrineQuery] = allRowsQuery(tables.shrineQueries)(_.toShrineQuery)

  protected lazy val queryResultRows: Query[QueryResultRow] = allRowsQuery(tables.queryResults)(_.toQueryResultRow(DefaultBreakdownResultOutputTypes.toSet))

  protected lazy val countResultRows: Query[CountRow] = allRowsQuery(tables.countResults)(_.toCountRow)

  protected lazy val breakdownResultRows: Query[BreakdownResultRow] = allRowsQuery(tables.breakdownResults)(_.toBreakdownResultRow)

  protected lazy val errorResultRows: Query[ShrineError] = allRowsQuery(tables.errorResults)(_.toShrineError)
}