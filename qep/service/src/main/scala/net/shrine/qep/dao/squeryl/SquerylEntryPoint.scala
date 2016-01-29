package net.shrine.qep.dao.squeryl

import org.squeryl.PrimitiveTypeMode
import net.shrine.qep.dao.model.AuditEntry
import org.squeryl.KeyedEntityDef

/**
 * @author clint
 * @date May 22, 2013
 */
object SquerylEntryPoint extends PrimitiveTypeMode {
  implicit val auditEntryKED: KeyedEntityDef[AuditEntry, Long] = new KeyedEntityDef[AuditEntry, Long] {
    override def getId(entry: AuditEntry): Long = entry.id
  
    override def isPersisted(entry: AuditEntry): Boolean = entry.id != 0
    
    override val idPropertyName: String = "id"
  }
}