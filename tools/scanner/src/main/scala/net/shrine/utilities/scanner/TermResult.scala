package net.shrine.utilities.scanner

import net.shrine.protocol.QueryResult

/**
 * @author clint
 * @date Mar 6, 2013
 */
sealed trait ScanQueryResult

final case class TermResult(networkQueryId: Long, networkResultId: Long, term: String, status: QueryResult.StatusType, count: Long) extends ScanQueryResult

final case class QueryFailure[T](input: T, cause: Throwable) extends ScanQueryResult