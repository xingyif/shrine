package net.shrine.utilities.batchquerier

import net.shrine.protocol.query.QueryDefinition
import net.shrine.protocol.version.v24.AggregatedRunQueryResponse

import scala.util.Try

/**
 * @author clint
 * @date Oct 9, 2013
 */
trait BatchQuerier {
  def query(queryDefs: Iterable[QueryDefinition], runsPerQueryDef: Int): Iterable[QueryAttempt]
}