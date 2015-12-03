package net.shrine.service.dao

import net.shrine.util.ShouldMatchersForJUnit
import org.springframework.beans.factory.annotation.Autowired
import net.shrine.service.dao.squeryl.tables.Tables
import net.shrine.service.dao.squeryl.SquerylEntryPoint

/**
 * @author clint
 * @date Mar 14, 2013
 */
trait AbstractAuditDaoTest extends Wiring with ShouldMatchersForJUnit {
  
  protected def afterMakingTables(f: => Any) {
    import SquerylEntryPoint._

    inTransaction {
      try {
        tables.auditEntries.schema.create

        f
      } finally {
        tables.auditEntries.schema.drop
      }
    }
  }
}