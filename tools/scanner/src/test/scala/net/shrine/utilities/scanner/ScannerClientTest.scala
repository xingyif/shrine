package net.shrine.utilities.scanner

import net.shrine.util.ShouldMatchersForJUnit
import org.junit.Test
import net.shrine.util.XmlDateHelper
import net.shrine.protocol.query.QueryDefinition
import net.shrine.protocol.query.Term
import net.shrine.protocol.query.QueryTiming

/**
 * @author clint
 * @date Feb 12, 2014
 */
final class ScannerClientTest extends ShouldMatchersForJUnit {
  val term = """\\FOO\Bar\Baz"""
  
  @Test
  def testMakeQueryName {
    val now = XmlDateHelper.now
      
    ScannerClient.makeQueryName(now, term) should equal(s"$now - $term")
  }
  
  @Test
  def testToQueryDef {
    val queryDef = ScannerClient.toQueryDef(term)
    
    val QueryDefinition(actualName, actualExpr, timing, id, queryType, constraints, subQueries) = queryDef
    
    actualName.endsWith(term) should be(true)
    actualExpr.get should equal(Term(term))
    timing.get should equal(QueryTiming.Any)
    id should be(None)
    queryType should be(None)
    constraints should be(None)
    subQueries should be(Nil)
  }
}