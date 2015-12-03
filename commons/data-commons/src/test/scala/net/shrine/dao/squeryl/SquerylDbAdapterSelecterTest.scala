package net.shrine.dao.squeryl

import net.shrine.util.ShouldMatchersForJUnit
import org.junit.Test
import org.squeryl.adapters.MySQLAdapter
import org.squeryl.adapters.MSSQLServer
import org.squeryl.adapters.OracleAdapter

/**
 * @author clint
 * @date Aug 13, 2013
 */
final class SquerylDbAdapterSelecterTest extends ShouldMatchersForJUnit {
  @Test
  def testDetermineAdapter {
    import SquerylDbAdapterSelecter.determineAdapter
    
    determineAdapter("MySQL").isInstanceOf[MySQLAdapter] should be(true)
    determineAdapter("sqlserver").isInstanceOf[MSSQLServer] should be(true)
    determineAdapter("Oracle").isInstanceOf[OracleAdapter] should be(true)
    
    intercept[IllegalArgumentException] {
      determineAdapter(null)
    }
    
    intercept[IllegalArgumentException] {
      determineAdapter("")
    }
    
    intercept[IllegalArgumentException] {
      determineAdapter("asdfasdf")
    }
  }
}