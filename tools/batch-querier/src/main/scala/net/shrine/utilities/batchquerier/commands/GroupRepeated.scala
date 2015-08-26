package net.shrine.utilities.batchquerier.commands

import net.shrine.utilities.commands.>>>
import net.shrine.utilities.batchquerier.BatchQueryResult
import net.shrine.utilities.batchquerier.RepeatedBatchQueryResult
import net.shrine.utilities.batchquerier.RepeatedBatchQueryResult

/**
 * @author clint
 * @date Oct 11, 2013
 */
final object GroupRepeated extends (Iterable[BatchQueryResult] >>> Iterable[RepeatedBatchQueryResult]) {
  override def apply(rawResults: Iterable[BatchQueryResult]): Iterable[RepeatedBatchQueryResult] = {
    val byInstAndQuery = rawResults.groupBy(r => (r.institution, r.query))
    
    val withAverages = byInstAndQuery.mapValues { resultsForInstAndQuery =>
      val runsPerQuery = resultsForInstAndQuery.size
      
      val meanDuration: Long = {
        //TODO: We just drop errors when computing average query time, is this ok?
        val totalTime = resultsForInstAndQuery.filter(_.elapsed.isFinite).map(_.elapsed.toMillis).sum
        
        (totalTime / runsPerQuery.toDouble).toLong
      }
      
      import scala.concurrent.duration._
      
      resultsForInstAndQuery.map(r => RepeatedBatchQueryResult(r.institution, r.query, r.disposition, r.elapsed, r.count, runsPerQuery, meanDuration.milliseconds))
    }
    
    withAverages.values.flatten
  }
}