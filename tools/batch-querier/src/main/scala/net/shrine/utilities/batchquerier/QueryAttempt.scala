package net.shrine.utilities.batchquerier

import net.shrine.protocol.AggregatedRunQueryResponse
import net.shrine.protocol.query.QueryDefinition

import scala.util.Try

/**
 * @author clint
 * @date Oct 16, 2013
 */
final case class QueryAttempt(queryDef: QueryDefinition, resultAttempt: Try[AggregatedRunQueryResponse])