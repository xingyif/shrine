package net.shrine.wiring

import org.springframework.jdbc.datasource.SingleConnectionDataSource
import org.squeryl.adapters.H2Adapter
import org.squeryl.internals.DatabaseAdapter

import javax.sql.DataSource
import net.shrine.dao.squeryl.DataSourceSquerylInitializer
import net.shrine.dao.squeryl.SquerylInitializer

/**
 * @author clint
 * @date Jan 16, 2014
 */
trait HasH2SquerylInitializer {
  val initializer: SquerylInitializer = HasH2SquerylInitializer.initializer
}

object HasH2SquerylInitializer {
  val initializer: SquerylInitializer = {

    val dataSource: DataSource = {
      val result = new SingleConnectionDataSource

      result.setDriverClassName("org.h2.Driver")
      result.setUrl("jdbc:h2:mem:testHibernate")
      result.setUsername("sa")
      result.setPassword("")
      result.setSuppressClose(true)

      result
    }

    val squerylAdapter: DatabaseAdapter = new H2Adapter

    new DataSourceSquerylInitializer(dataSource, squerylAdapter)
  }
}