package net.shrine.protocol.query

import org.junit.Test
import net.shrine.util.ShouldMatchersForJUnit

/**
 * @author clint
 * @date Sep 24, 2014
 */
final class QueryTimingTest extends ShouldMatchersForJUnit {
  @Test
  def testName: Unit = {
    import QueryTiming._

    Any.name should equal("ANY")
    SameVisit.name should equal("SAMEVISIT")
    SameInstanceNum.name should equal("SAMEINSTANCENUM")
  }
  
  @Test
  def testIsAny: Unit = {
    import QueryTiming._
    
    Any.isAny should be(true)
    SameVisit.isAny should be(false)
    SameInstanceNum.isAny should be(false)
  }
}

