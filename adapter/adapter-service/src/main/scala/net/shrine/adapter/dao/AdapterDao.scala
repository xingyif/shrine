package net.shrine.adapter.dao

import net.shrine.protocol.query.QueryDefinition
import net.shrine.protocol.AuthenticationInfo
import net.shrine.adapter.dao.model.ShrineQueryResult
import net.shrine.protocol.QueryResult
import net.shrine.protocol.I2b2ResultEnvelope
import net.shrine.protocol.ResultOutputType
import net.shrine.adapter.dao.model.ShrineQuery

import scala.xml.NodeSeq

/**
 * @author clint
 * @since Oct 15, 2012
 */
trait AdapterDao {
  /**
   * @return the id column of the inserted row
   */
  def insertQuery(masterId: String,
                  networkId: Long,
                  authn: AuthenticationInfo,
                  queryDefinition: QueryDefinition,
                  isFlagged: Boolean,
                  hasBeenRun: Boolean,
                  flagMessage: Option[String]): Int

  //Returns a Map of output types to Seqs of inserted ids, since the ERROR output type can be used for multiple query_result rows,
  //Say for a run query operation that results in multiple error responses from the CRC.
  def insertQueryResults(parentQueryId: Int, results: Seq[QueryResult]): Map[ResultOutputType, Seq[Int]]
  
  def insertCountResult(resultId: Int, originalCount: Long, obfuscatedCount: Long): Unit
  
  def insertBreakdownResults(parentResultIds: Map[ResultOutputType, Seq[Int]], originalBreakdowns: Map[ResultOutputType, I2b2ResultEnvelope], obfuscatedBreakdowns: Map[ResultOutputType, I2b2ResultEnvelope]): Unit

  def insertErrorResult(parentResultId: Int, errorMessage: String, codec:String, stampText:String, summary:String, digestDescription:String,detailsXml:NodeSeq): Unit
  
  def findQueriesByUserAndDomain(domain: String, username: String, howMany: Int): Seq[ShrineQuery]
  
  def findQueryByNetworkId(networkQueryId: Long): Option[ShrineQuery]
  
  def findResultsFor(networkQueryId: Long): Option[ShrineQueryResult]
  
  def isUserLockedOut(authn: AuthenticationInfo, defaultThreshold: Int): Boolean
  
  def renameQuery(networkQueryId: Long, newName: String): Unit
  
  def deleteQuery(networkQueryId: Long): Unit
  
  def deleteQueryResultsFor(networkQueryId: Long): Unit
  
  def findRecentQueries(howMany: Int): Seq[ShrineQuery]
  
  def storeResults(authn: AuthenticationInfo,
                   masterId: String,
                   networkQueryId: Long,
                   queryDefinition: QueryDefinition,
                   rawQueryResults: Seq[QueryResult],
                   obfuscatedQueryResults: Seq[QueryResult],
                   failedBreakdownTypes: Seq[ResultOutputType],
                   mergedBreakdowns: Map[ResultOutputType, I2b2ResultEnvelope],
                   obfuscatedBreakdowns: Map[ResultOutputType, I2b2ResultEnvelope]): Unit
  
  def flagQuery(networkQueryId: Long, message: Option[String]): Unit
  
  def unFlagQuery(networkQueryId: Long): Unit
                   
  def inTransaction[T](f: => T): T = f
  
}
