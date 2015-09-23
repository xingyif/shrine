package net.shrine.adapter.dao.model

import net.shrine.protocol.QueryResult

/**
 * @author clint
 * @since Oct 16, 2012
 * 
 */
final case class ShrineError(id: Int, resultId: Int, message: String) extends HasResultId {
  def toQueryResult: QueryResult = {
    QueryResult.errorResult(Option(message), QueryResult.StatusType.Error.name)
  }
}
