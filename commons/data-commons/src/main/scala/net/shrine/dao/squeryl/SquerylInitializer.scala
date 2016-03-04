package net.shrine.dao.squeryl

import org.squeryl.internals.DatabaseAdapter
import org.squeryl.SessionFactory
import org.squeryl.Session
import javax.sql.DataSource

/**
 * @author clint
 * @date May 21, 2013
 */

trait SquerylInitializer {
  def init(): Unit
}