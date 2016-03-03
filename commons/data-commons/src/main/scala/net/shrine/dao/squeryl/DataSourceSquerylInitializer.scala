package net.shrine.dao.squeryl

import org.squeryl.internals.DatabaseAdapter
import org.squeryl.SessionFactory
import org.squeryl.Session
import javax.sql.DataSource

/**
 * @author clint
 * @since May 21, 2013
 */
final class DataSourceSquerylInitializer(dataSource: DataSource, adapter: DatabaseAdapter) extends SquerylInitializer {
  override lazy val init: Unit = {
    SessionFactory.concreteFactory = Some { () =>
      Session.create(dataSource.getConnection, adapter)
    }
  }
}