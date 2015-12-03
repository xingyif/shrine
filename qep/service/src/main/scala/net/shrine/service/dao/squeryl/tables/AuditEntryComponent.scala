package net.shrine.service.dao.squeryl.tables

import org.squeryl.Schema
import net.shrine.service.dao.model.AuditEntry
import net.shrine.service.dao.squeryl.SquerylEntryPoint
import net.shrine.dao.squeryl.tables.AbstractTableComponent

/**
 * @author clint
 * @date May 21, 2013
 */
trait AuditEntryComponent extends AbstractTableComponent { self: Schema =>
  import SquerylEntryPoint._

  val auditEntries = table[AuditEntry]("AUDIT_ENTRY")

  declareThat(auditEntries) (
    _.id is (primaryKey, oracleSafeAutoIncremented("AUDIT_ENTRY_ID"), named("AUDIT_ENTRY_ID")),
    _.project is (named("PROJECT")),
    _.queryTopic is (named("QUERY_TOPIC")),
    _.queryTopic is (named("QUERY_TOPIC")),
    _.username is (named("USERNAME")),
    _.domain is (named("DOMAIN_NAME")),
    //Still can't express non-constant default value for TIME column :(
    _.time is (named("TIME")),
    _.queryText is (dbType("TEXT"), named("QUERY_TEXT")),
    auditEntry => columns(auditEntry.domain, auditEntry.username, auditEntry.queryTopic) are (indexed("IDX_AUDIT_ENTRY_DOMAIN_USERNAME_QUERY_TOPIC"))
  )
}