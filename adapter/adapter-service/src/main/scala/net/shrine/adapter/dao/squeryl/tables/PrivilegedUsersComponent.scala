package net.shrine.adapter.dao.squeryl.tables

import org.squeryl.Schema
import net.shrine.adapter.dao.model.squeryl.SquerylPrivilegedUser
import net.shrine.dao.squeryl.tables.AbstractTableComponent
import net.shrine.dao.squeryl.SquerylEntryPoint

/**
 * @author clint
 * @since May 22, 2013
 */
trait PrivilegedUsersComponent extends AbstractTableComponent { self: Schema =>
  import SquerylEntryPoint._

  val privilegedUsers = table[SquerylPrivilegedUser]("PRIVILEGED_USER")

  declareThat(privilegedUsers) (
    _.id is (primaryKey, oracleSafeAutoIncremented("PRIV_USER_ID")),
    user => columns(user.username, user.domain) are (indexed, unique)
  )
}