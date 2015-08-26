package net.shrine.broadcaster

import net.shrine.dao.squeryl.SquerylInitializer
import net.shrine.wiring.HasH2SquerylInitializer
import net.shrine.broadcaster.dao.squeryl.tables.Tables
import net.shrine.broadcaster.dao.HubDao
import net.shrine.broadcaster.dao.squeryl.SquerylHubDao

/**
 * @author clint
 * @date Dec 15, 2014
 */
trait Wiring {
  def initializer: SquerylInitializer = Wiring.initializer
  
  def tables: Tables = Wiring.tables

  def dao: HubDao = Wiring.dao
}

object Wiring extends HasH2SquerylInitializer  {
  val tables = new Tables

  val dao: HubDao = new SquerylHubDao(initializer, tables)
}