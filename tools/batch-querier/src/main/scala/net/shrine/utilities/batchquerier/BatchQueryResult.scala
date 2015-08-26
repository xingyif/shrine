package net.shrine.utilities.batchquerier

import net.shrine.protocol.query.QueryDefinition
import scala.concurrent.duration.Duration
import net.shrine.protocol.RunQueryResponse
import net.shrine.protocol.QueryResult

/**
 * @author clint
 * @date Sep 10, 2013
 */
final case class BatchQueryResult(institution: String, query: QueryDefinition, disposition: QueryResult.StatusType, elapsed: Duration, count: Long)
