package net.shrine.dao.squeryl

import org.squeryl.SessionFactory
import org.squeryl.Session
import java.sql.DriverManager
import org.squeryl.internals.DatabaseAdapter

/**
 * @author clint
 * @date May 29, 2013
 */
final class JdbcUrlSquerylInitializer(adapter: DatabaseAdapter, jdbcDriverClass: String, jdbcUrl: String, user: String, password: String) extends SquerylInitializer {
  override lazy val init: Unit = {
    Class.forName(jdbcDriverClass)
    
    SessionFactory.concreteFactory = Some { () =>
      Session.create(DriverManager.getConnection(jdbcUrl, user, password), adapter)
    }
  }
}