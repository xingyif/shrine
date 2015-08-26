package net.shrine.utilities.batchquerier

import net.shrine.protocol.query.QueryDefinition
import scala.concurrent.duration.Duration
import net.shrine.protocol.QueryResult

/**
 * @author clint
 * @date Oct 11, 2013
 */
final case class RepeatedBatchQueryResult(
    institution: String, 
    query: QueryDefinition, 
    disposition: QueryResult.StatusType,
    elapsed: Duration, 
    count: Long, 
    numQueriesPerformed: Int, 
    meanDuration: Duration)