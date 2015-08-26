package net.shrine.adapter

import net.shrine.adapter.dao.I2b2AdminDao
import net.shrine.adapter.dao.squeryl.AbstractSquerylAdapterTest
import net.shrine.adapter.dao.squeryl.SquerylI2b2AdminDao

/**
 * @author clint
 * @date Apr 24, 2013
 */
trait HasI2b2AdminDao extends Wiring { self: AbstractSquerylAdapterTest =>
  protected val shrineProjectId = "SHRINE"  
  
  protected def i2b2AdminDao: I2b2AdminDao = new SquerylI2b2AdminDao(shrineProjectId, initializer, tables)
}