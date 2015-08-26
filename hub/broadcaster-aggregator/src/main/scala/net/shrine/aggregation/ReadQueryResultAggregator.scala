package net.shrine.aggregation

import net.shrine.protocol.ReadQueryResultResponse
import net.shrine.protocol.AggregatedReadQueryResultResponse
import net.shrine.protocol.QueryResult
import net.shrine.protocol.QueryResult.StatusType.Finished
import net.shrine.protocol.ResultOutputType.PATIENT_COUNT_XML

/**
 * @author clint
 * @date Nov 6, 2012
 */
//TODO: TEST!!!
final class ReadQueryResultAggregator(val shrineNetworkQueryId: Long, val showAggregation: Boolean) extends 
    StoredResultsAggregator[ReadQueryResultResponse, AggregatedReadQueryResultResponse](
        shrineNetworkQueryId: Long, 
        showAggregation: Boolean,
        Some("No results available"), 
        Some("No results available")) {
  
  def withShrineNetworkQueryId(qId: Long) = new ReadQueryResultAggregator(qId, showAggregation)
  
  protected override def consolidateQueryResults(queryResultsFromAllValidResponses: Iterable[(BasicAggregator.Valid[ReadQueryResultResponse], Iterable[QueryResult])]): Iterable[QueryResult] = {
    //Lose the meta data, make a concatenated list of all the QueryResults
    queryResultsFromAllValidResponses.unzip._2.flatten
  }
  
  protected override def makeAggregatedResult(queryResults: Iterable[QueryResult]): Option[QueryResult] = {
    val totalSize = queryResults.map(_.setSize).sum
          
    queryResults.headOption.map(_.copy(instanceId = shrineNetworkQueryId, resultType = Some(PATIENT_COUNT_XML), setSize = totalSize, description = Some("Aggregated Count"), statusType = Finished, statusMessage = None))
  }
}
