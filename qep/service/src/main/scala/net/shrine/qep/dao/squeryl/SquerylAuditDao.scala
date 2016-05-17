package net.shrine.qep.dao.squeryl

import net.shrine.qep.dao.AuditDao
import net.shrine.qep.dao.squeryl.tables.Tables
import java.util.Date
import java.sql.Timestamp
import net.shrine.qep.dao.model.AuditEntry
import org.squeryl.KeyedEntity
import net.shrine.dao.squeryl.SquerylInitializer

/**
 * @author clint
 * @date May 21, 2013
 */
final class SquerylAuditDao(initializer: SquerylInitializer, tables: Tables) extends AuditDao {

  initializer.init
  
  override def inTransaction[T](f: => T): T = SquerylEntryPoint.inTransaction(f)
  
  import SquerylEntryPoint._
  import tables.auditEntries
  
  override def addAuditEntry(time: Date, project: String, domain: String, username: String, queryText: String, queryTopic: Option[String]) {
    val timestamp = new Timestamp(time.getTime)

    inTransaction { 
      //NB: Squeryl steers us toward inserting with dummy ids :(
      auditEntries.insert(AuditEntry(0L, project, domain, username, timestamp, Option(queryText), queryTopic))
    }
  }

  override def findRecentEntries(limit: Int): Seq[AuditEntry] = {
    inTransaction {
      from(auditEntries) { entry => 
        select(entry).orderBy(entry.time.desc)
      }.take(limit).toSeq
    }
  }
}