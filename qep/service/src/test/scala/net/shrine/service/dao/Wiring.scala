package net.shrine.service.dao

import net.shrine.service.dao.squeryl.SquerylAuditDao
import net.shrine.service.dao.squeryl.tables.Tables
import net.shrine.wiring.HasH2SquerylInitializer

/**
 * @author clint
 * @date Jan 15, 2014
 */
trait Wiring {
  val auditDao: AuditDao = Wiring.auditDao

  val tables: Tables = Wiring.tables
}

object Wiring extends HasH2SquerylInitializer {
  lazy val auditDao: AuditDao = new SquerylAuditDao(initializer, tables)

  lazy val tables: Tables = new Tables
}