package net.shrine.aggregation

import net.shrine.aggregation.BasicAggregator.Valid
import net.shrine.protocol.SingleNodeReadTranslatedQueryDefinitionResponse
import net.shrine.protocol.QueryResult
import net.shrine.protocol.ShrineResponse
import net.shrine.protocol.AggregatedReadTranslatedQueryDefinitionResponse
import net.shrine.protocol.ErrorResponse
import net.shrine.protocol.BaseShrineResponse

/**
 * @author clint
 * @date Feb 14, 2014
 */
final class ReadTranslatedQueryDefinitionAggregator extends IgnoresErrorsAggregator[SingleNodeReadTranslatedQueryDefinitionResponse] {
  override private[aggregation] def makeResponseFrom(validResponses: Iterable[Valid[SingleNodeReadTranslatedQueryDefinitionResponse]]): BaseShrineResponse = {
    if(validResponses.isEmpty) { ErrorResponse("No valid responses to aggregate") }
    else {
      val singleNodeResults = validResponses.map(_.response.result).toSeq
      
      AggregatedReadTranslatedQueryDefinitionResponse(singleNodeResults)
    }
  }
}