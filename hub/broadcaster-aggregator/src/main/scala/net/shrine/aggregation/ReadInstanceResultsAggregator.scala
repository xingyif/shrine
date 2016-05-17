package net.shrine.aggregation

import net.shrine.protocol.AggregatedReadInstanceResultsResponse
import net.shrine.protocol.NodeId
import net.shrine.protocol.QueryResult
import net.shrine.protocol.ReadInstanceResultsResponse
import net.shrine.protocol.ResultOutputType

/**
 * @author Bill Simons
 * @author Clint Gilbert
 * @since 6/13/11
 * @see http://cbmi.med.harvard.edu
 * @see http://chip.org
 *       <p/>
 *       NOTICE: This software comes with NO guarantees whatsoever and is
 *       licensed as Lgpl Open Source
 * @see http://www.gnu.org/licenses/lgpl.html
 */
final class ReadInstanceResultsAggregator(shrineNetworkQueryId: Long, showAggregation: Boolean) extends 
    StoredResultsAggregator[ReadInstanceResultsResponse, AggregatedReadInstanceResultsResponse](
        shrineNetworkQueryId, 
        showAggregation,
        Some("No results available"), 
        Some("No results available")) {

  import ResultOutputType._
  
  private val setType = Some(PATIENT_COUNT_XML)
  private val statusType = QueryResult.StatusType.Finished

  protected override def consolidateQueryResults(queryResultsFromAllValidResponses: Iterable[(BasicAggregator.Valid[ReadInstanceResultsResponse], Iterable[QueryResult])]): Iterable[QueryResult] = {
    queryResultsFromAllValidResponses.flatMap { case (validResult, resultsFromOneResponse) =>
      for {
        firstResult <- resultsFromOneResponse.headOption
        newResult = firstResult.withResultType(PATIENT_COUNT_XML) //Eh?
      } yield {
        transformResult(newResult, validResult.origin)
      }
    }
  }
  
  protected override def makeAggregatedResult(queryResults: Iterable[QueryResult]): Option[QueryResult] = {
    val totalSize = queryResults.map(_.setSize).sum

    queryResults.headOption.map(_.copy(instanceId = shrineNetworkQueryId, resultType = setType, setSize = totalSize, description = Some("Aggregated Count"), statusType = statusType, statusMessage = None))
  }
  
  /**
   * Default implementation only replaces the description with the spinResult description; Subclasses can override
   * and do something more interesting.
   */
  //TODO XXX: Get description/node name some better way
  protected[aggregation] def transformResult(queryResult: QueryResult, origin: NodeId): QueryResult = queryResult.withDescription(origin.name)
}