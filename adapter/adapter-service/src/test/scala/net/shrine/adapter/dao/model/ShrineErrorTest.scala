package net.shrine.adapter.dao.model

import net.shrine.util.ShouldMatchersForJUnit
import net.shrine.protocol.QueryResult

/**
 * @author clint
 * @date Nov 1, 2012
 */
final class ShrineErrorTest extends ShouldMatchersForJUnit {
  def testToQueryResult {
    val message = "something broke"
    
    val error = ShrineError(1, 123, message)  
    
    error.toQueryResult should equal(QueryResult.errorResult(Some(message), QueryResult.StatusType.Error.name))
  }
}