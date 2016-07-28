package net.shrine.adapter.components

import net.shrine.adapter.dao.AdapterDao
import net.shrine.problem.{AbstractProblem, ProblemSources}
import net.shrine.protocol.ShrineResponse
import net.shrine.protocol.ReadQueryDefinitionRequest
import net.shrine.protocol.ReadQueryDefinitionResponse
import net.shrine.protocol.ErrorResponse
import net.shrine.protocol.query.QueryDefinition
import net.shrine.protocol.AbstractReadQueryDefinitionRequest

/**
 * @author clint
 * @since Apr 4, 2013
 *
 * NB: Tested by ReadQueryDefinitionAdapterTest
 */
final case class QueryDefinitions[Req <: AbstractReadQueryDefinitionRequest](dao: AdapterDao) {
  def get(request: Req): ShrineResponse = {
    val resultOption = for {
      shrineQuery <- dao.findQueryByNetworkId(request.queryId)
    } yield {
      ReadQueryDefinitionResponse(
        shrineQuery.networkId,
        shrineQuery.name,
        shrineQuery.username,
        shrineQuery.dateCreated,
        //TODO: I2b2 or Shrine format?
        shrineQuery.queryDefinition.toI2b2String)
    }

    resultOption.getOrElse(ErrorResponse(QueryNotInDatabase(request)))
  }
}

case class QueryNotInDatabase(request:AbstractReadQueryDefinitionRequest) extends AbstractProblem(ProblemSources.Hub) {
  override val summary: String = s"Couldn't find query definition."
  override val description:String = s"The query definition with network id: ${request.queryId} does not exist at this site."
}