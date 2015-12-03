package net.shrine.adapter

import net.shrine.adapter.dao.AdapterDao
import net.shrine.adapter.dao.squeryl.SquerylAdapterDao
import net.shrine.adapter.dao.squeryl.tables.Tables
import net.shrine.dao.squeryl.SquerylInitializer
import net.shrine.wiring.HasH2SquerylInitializer
import net.shrine.adapter.dao.I2b2AdminDao
import net.shrine.adapter.dao.squeryl.SquerylI2b2AdminDao
import net.shrine.protocol.DefaultBreakdownResultOutputTypes

/**
 * @author clint
 * @date Jan 15, 2014
 */
trait Wiring {
  val initializer: SquerylInitializer = Wiring.initializer
  
  val tables: Tables = Wiring.tables

  val dao: AdapterDao = Wiring.dao
}

object Wiring extends HasH2SquerylInitializer  {
  val tables = new Tables

  val dao: AdapterDao = new SquerylAdapterDao(initializer, tables)(DefaultBreakdownResultOutputTypes.toSet)
}