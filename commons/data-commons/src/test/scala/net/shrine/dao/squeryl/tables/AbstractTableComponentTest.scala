package net.shrine.dao.squeryl.tables

import net.shrine.util.ShouldMatchersForJUnit
import org.junit.Test
import org.squeryl.Schema
import net.shrine.dao.squeryl.SquerylEntryPoint

/**
 * @author clint
 * @date Aug 13, 2013
 */
final class AbstractTableComponentTest extends ShouldMatchersForJUnit {
  @Test
  def testOracleSafeAutoIncremented {
    import SquerylEntryPoint._
    
    object MockTableComponent extends Schema with AbstractTableComponent {
      intercept[IllegalArgumentException] {
        oracleSafeAutoIncremented(null)
      }
      
      intercept[IllegalArgumentException] {
        oracleSafeAutoIncremented("x" * 30)
      }
      
      intercept[IllegalArgumentException] {
        oracleSafeAutoIncremented("x" * 31)
      }
      
      intercept[IllegalArgumentException] {
        oracleSafeAutoIncremented("x" * 100)
      }
      
      oracleSafeAutoIncremented("x" * 29) should equal(autoIncremented("x" * 29))

      oracleSafeAutoIncremented("foo") should equal(autoIncremented("foo"))
    } 
  }
}