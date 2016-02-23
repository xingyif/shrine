package net.shrine.aggregation

import net.shrine.aggregation.BasicAggregator.Valid
import net.shrine.protocol.AggregatedRunQueryResponse
import net.shrine.protocol.QueryResult
import net.shrine.protocol.ResultOutputType
import net.shrine.protocol.RunQueryResponse
import net.shrine.protocol.ShrineResponse
import net.shrine.protocol.query.QueryDefinition
import net.shrine.util.XmlDateHelper

/**
 *
 *
 * @author Justin Quan
 * @author Clint Gilbert
 * @see http://chip.org
 * @since 8/11/11
 */
final class RunQueryAggregator(
  val queryId: Long,
  val userId: String,
  val groupId: String,
  val requestQueryDefinition: QueryDefinition,
  val addAggregatedResult: Boolean) extends PackagesErrorsAggregator[RunQueryResponse](errorMessage = None, invalidMessage = Some("Unexpected response")) {

  def withQueryId(qId: Long) = new RunQueryAggregator(qId, userId, groupId, requestQueryDefinition, addAggregatedResult)
  
  //TODO XXX: Get description/node name some better way
  private def transformResult(queryResult: QueryResult, metaData: Valid[RunQueryResponse]): QueryResult = queryResult.withDescription(metaData.origin.name)

  import RunQueryAggregator._
  
  private[aggregation] override def makeResponse(validResponses: Iterable[Valid[RunQueryResponse]], errorResponses: Iterable[QueryResult], invalidResponses: Iterable[QueryResult]): ShrineResponse = {

    val results = validResponses.flatMap {
      case result @ Valid(origin, elapsed, response) =>
        response.results.map(transformResult(_, result))
    }

    import ResultOutputType._

    val counts = validResponses.map {
      case Valid(origin, elsapsed, response) =>
        val countResultOption = response.results.find(_.resultTypeIs(PATIENT_COUNT_XML)).map(_.setSize)

        countResultOption.getOrElse(0L)
    }

    val now = XmlDateHelper.now

    val aggResults = {
      if (addAggregatedResult) {
        val sumResult = new QueryResult(0L, invalidInstanceId, PATIENT_COUNT_XML, counts.sum, now, now, "TOTAL COUNT", QueryResult.StatusType.Finished)

        results.toSeq :+ sumResult
      } else {
        results
      }
    }

    AggregatedRunQueryResponse(queryId, now, userId, groupId, requestQueryDefinition, invalidInstanceId, aggResults.toSeq ++ errorResponses ++ invalidResponses)
  }
}

object RunQueryAggregator {
  val invalidInstanceId = -1L
}