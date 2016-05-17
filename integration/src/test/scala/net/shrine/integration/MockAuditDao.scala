package net.shrine.integration

import net.shrine.qep.dao.AuditDao
import java.util.Date
import net.shrine.qep.dao.model.AuditEntry

/**
 * @author clint
 * @date Nov 27, 2013
 */
object MockAuditDao extends AuditDao {
  override def addAuditEntry(
    time: Date,
    project: String,
    domain: String,
    username: String,
    queryText: String,
    queryTopic: Option[String]): Unit = ()

  override def findRecentEntries(limit: Int): Seq[AuditEntry] = Nil

  override def inTransaction[T](f: => T): T = f
}