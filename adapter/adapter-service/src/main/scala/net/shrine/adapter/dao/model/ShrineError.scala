package net.shrine.adapter.dao.model

import net.shrine.protocol.QueryResult

/**
 * @author clint
 * @date Oct 16, 2012
 * 
 * NB: Named ShrineError to avoid clashes with java.lang.Error
 * NB: Can't be final, since Squeryl runs this class through cglib to make a synthetic subclass :(
 */
final case class ShrineError(id: Int, resultId: Int, message: String) extends HasResultId {
  def toQueryResult: QueryResult = {
    QueryResult.errorResult(Option(message), QueryResult.StatusType.Error.name)
  }
}
