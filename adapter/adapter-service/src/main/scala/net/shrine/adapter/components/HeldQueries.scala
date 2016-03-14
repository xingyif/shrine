package net.shrine.adapter.components

import net.shrine.protocol.RunHeldQueryRequest
import net.shrine.protocol.ShrineResponse
import net.shrine.protocol.BroadcastMessage
import net.shrine.adapter.dao.model.ShrineQueryResult
import net.shrine.protocol.RunQueryRequest
import net.shrine.protocol.ResultOutputType
import net.shrine.protocol.ErrorResponse
import net.shrine.protocol.query.QueryDefinition
import net.shrine.protocol.RunQueryResponse
import net.shrine.adapter.dao.AdapterDao
import net.shrine.adapter.{QueryNotFound, RunQueryAdapter}
import net.shrine.protocol.AuthenticationInfo
import net.shrine.protocol.Credential

/**
 * @author clint
 * @since May 2, 2014
 */
final case class HeldQueries(dao: AdapterDao, runQueryAdapter: RunQueryAdapter) {
  def run(req: RunHeldQueryRequest): ShrineResponse = {
    val queryId = req.networkQueryId

    //TODO: Revisit this, we might want to store/retrieve the output types used originally.
    val outputTypes: Set[ResultOutputType] = Set(ResultOutputType.PATIENT_COUNT_XML)

    dao.findQueryByNetworkId(queryId) match {
      case Some(savedQuery) => {
        //Re-un the query with the original credentials, not an admin's
        val savedAuthn = AuthenticationInfo(savedQuery.domain, savedQuery.username, Credential("", false))
        
        val runQueryReq = RunQueryRequest(
          req.projectId,
          req.waitTime,
          savedAuthn,
          queryId,
          topicId = None,
          topicName = None,
          outputTypes,
          savedQuery.queryDefinition)

        val newBroadcastMessage = BroadcastMessage(savedAuthn, runQueryReq)

        dao.inTransaction {
          //Delete previous records for this query from the DB, so we don't have obsolete records with
          //SHRINE_QUERY.HAS_BEEN_RUN = false for queries like the current one that ended up getting run.
          //Invoking runQueryAdapter.processRequest() will add correct values to the DB for the
          //actually-got-run query.
          dao.deleteQueryResultsFor(queryId)

          dao.deleteQuery(queryId)

          runQueryAdapter.processRequest(newBroadcastMessage)
        }
      }
      case None => ErrorResponse(QueryNotFound(queryId))
    }
  }
}