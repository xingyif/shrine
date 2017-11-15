package net.shrine.utilities.batchquerier.commands

import net.shrine.protocol.AggregatedRunQueryResponse
import net.shrine.protocol.query.QueryDefinition
import net.shrine.utilities.batchquerier.BatchQuerier
import net.shrine.utilities.batchquerier.BatchQueryResult
import net.shrine.utilities.batchquerier.BatchQueryResults
import net.shrine.utilities.commands.>>>

import scala.util.Failure
import scala.util.Success
import net.shrine.utilities.batchquerier.QueryAttempt

/**
 * @author clint
 * @date Sep 20, 2013
 */
final case class QueryWith(querier: BatchQuerier, runsPerQueryDef: Int) extends (Iterable[QueryDefinition] >>> Iterable[BatchQueryResult]) {
  
  override def apply(queryDefs: Iterable[QueryDefinition]): Iterable[BatchQueryResult] = {
    val attempts = querier.query(queryDefs, runsPerQueryDef)
    
    val (succeeded, failed) = attempts.partition(_.resultAttempt.isSuccess)
    
    val resultsForSuccesses = for {
      QueryAttempt(_, Success(aggregatedRunQueryResponse)) <- succeeded
      result <- BatchQueryResults.fromAggregatedRunQueryResponse(aggregatedRunQueryResponse)
    } yield result
    
    val resultsForErrors = for {
      QueryAttempt(queryDef, Failure(_)) <- failed
    } yield BatchQueryResults.forFailure(queryDef)
    
    resultsForSuccesses ++ resultsForErrors
  }
  
  override def toString = s"QueryWith($querier)"
}
