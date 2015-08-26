package net.shrine.adapter

import net.shrine.serialization.XmlMarshaller
import net.shrine.adapter.dao.AdapterDao
import net.shrine.protocol.QueryResult
import net.shrine.protocol.ErrorResponse
import net.shrine.adapter.dao.model.ShrineQueryResult

/**
 * @author clint
 * @date Nov 6, 2012
 */
object StoredQueries {
  private[adapter] def retrieve(dao: AdapterDao, queryId: Long): Option[ShrineQueryResult] = {
    val result = dao.findResultsFor(queryId)
    
    result
  }
  
  private[adapter] def retrieveAsQueryResult(dao: AdapterDao, doObfuscation: Boolean, queryId: Long): Option[QueryResult] = {
    for {
      shrineResults <- retrieve(dao, queryId)
      queryResult <- shrineResults.toQueryResults(doObfuscation)
    } yield queryResult
  }
}