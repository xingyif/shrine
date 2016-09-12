package net.shrine.adapter.dao

import net.shrine.adapter.dao.model.ShrineQuery
import net.shrine.adapter.dao.model.ShrineQueryResult
import net.shrine.protocol.AuthenticationInfo
import net.shrine.protocol.I2b2ResultEnvelope
import net.shrine.protocol.QueryResult
import net.shrine.protocol.ResultOutputType
import net.shrine.protocol.query.QueryDefinition

import scala.concurrent.duration.Duration
import scala.xml.NodeSeq

/**
 * @author clint
 * @since Oct 19, 2012
 */
object MockAdapterDao extends MockAdapterDao

trait MockAdapterDao extends AdapterDao {
  override def flagQuery(networkQueryId: Long, flagMessage: Option[String]): Unit = ()
  
  override def unFlagQuery(networkQueryId: Long): Unit = ()
  
  override def insertQuery(localMasterId: String, networkId: Long, authn: AuthenticationInfo, query: QueryDefinition, isFlagged: Boolean, hasBeenRun: Boolean, flagMessage: Option[String]): Int = 0

  override def insertQueryResults(parentQueryId: Int, results: Seq[QueryResult]): Map[ResultOutputType, Seq[Int]] = Map.empty

  override def insertCountResult(resultId: Int, originalCount: Long, obfuscatedCount: Long): Unit = ()

  override def insertBreakdownResults(parentResultIds: Map[ResultOutputType, Seq[Int]], originalBreakdowns: Map[ResultOutputType, I2b2ResultEnvelope], obfuscatedBreakdowns: Map[ResultOutputType, I2b2ResultEnvelope]): Unit = ()

  override def insertErrorResult(parentResultId: Int, errorMessage: String, codec:String, stampText:String, summary:String, digestDescription:String,detailsXml:NodeSeq) = ()

  override def findQueryByNetworkId(networkQueryId: Long): Option[ShrineQuery] = None

  override def findQueriesByUserAndDomain(domain: String, username: String, howMany: Int): Seq[ShrineQuery] = Nil

  override def findQueriesByDomain(domain: String): Seq[ShrineQuery] = Nil

  override def findResultsFor(networkQueryId: Long): Option[ShrineQueryResult] = None

  override def checkIfBot(authn:AuthenticationInfo, botTimeThresholds:Seq[(Long,Duration)]): Unit = {}

  override def isUserLockedOut(id: AuthenticationInfo, defaultThreshold: Int): Boolean = false

  override def renameQuery(networkQueryId: Long, newName: String): Unit = ()

  override def deleteQuery(networkQueryId: Long): Unit = ()
  
  override def deleteQueryResultsFor(networkQueryId: Long): Unit = ()

  override def findRecentQueries(howMany: Int): Seq[ShrineQuery] = Nil

  override def storeResults(authn: AuthenticationInfo,
                            masterId: String,
                            networkQueryId: Long,
                            queryDefinition: QueryDefinition,
                            rawQueryResults: Seq[QueryResult],
                            obfuscatedQueryResults: Seq[QueryResult],
                            failedBreakdownTypes: Seq[ResultOutputType],
                            mergedBreakdowns: Map[ResultOutputType, I2b2ResultEnvelope],
                            obfuscatedBreakdowns: Map[ResultOutputType, I2b2ResultEnvelope]): Unit = ()
}