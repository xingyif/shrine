package net.shrine.aggregation

import net.shrine.aggregation.BasicAggregator.Valid
import net.shrine.protocol.{AggregatedReadTranslatedQueryDefinitionResponse, BaseShrineResponse, BroadcastMessage, ErrorResponse, QueryResult, ShrineResponse, SingleNodeReadTranslatedQueryDefinitionResponse}

/**
 * @author clint
 * @since Feb 14, 2014
 */
final class ReadTranslatedQueryDefinitionAggregator extends IgnoresErrorsAggregator[SingleNodeReadTranslatedQueryDefinitionResponse] {

  override private[aggregation] def makeResponseFrom(validResponses: Iterable[Valid[SingleNodeReadTranslatedQueryDefinitionResponse]],respondingTo: BroadcastMessage): BaseShrineResponse = {
    if(validResponses.isEmpty) { ErrorResponse(NoValidResponsesToAggregate(respondingTo.request.requestType,respondingTo.networkAuthn.username,respondingTo.networkAuthn.domain)) }
    else {
      val singleNodeResults = validResponses.map(_.response.result).toSeq
      
      AggregatedReadTranslatedQueryDefinitionResponse(singleNodeResults)
    }
  }
}