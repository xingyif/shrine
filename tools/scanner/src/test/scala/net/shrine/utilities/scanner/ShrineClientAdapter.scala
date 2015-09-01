package net.shrine.utilities.scanner

import net.shrine.client.ShrineClient
import net.shrine.protocol.ResultOutputType
import scala.xml.NodeSeq
import net.shrine.protocol.ReadApprovedQueryTopicsResponse
import net.shrine.protocol.ReadPreviousQueriesResponse
import net.shrine.protocol.AggregatedRunQueryResponse
import net.shrine.protocol.ReadQueryInstancesResponse
import net.shrine.protocol.AggregatedReadInstanceResultsResponse
import net.shrine.protocol.ReadPdoResponse
import net.shrine.protocol.ReadQueryDefinitionResponse
import net.shrine.protocol.DeleteQueryResponse
import net.shrine.protocol.RenameQueryResponse
import net.shrine.protocol.AggregatedReadQueryResultResponse
import net.shrine.protocol.query.QueryDefinition
import net.shrine.protocol.AggregatedReadTranslatedQueryDefinitionResponse
import net.shrine.protocol.FlagQueryResponse
import net.shrine.protocol.UnFlagQueryResponse

/**
 * @author clint
 * @since Mar 7, 2013
 */
abstract class ShrineClientAdapter extends ShrineClient {
  override def readApprovedQueryTopics(userId: String, shouldBroadcast: Boolean): ReadApprovedQueryTopicsResponse = null

  override def readPreviousQueries(userId: String, fetchSize: Int, shouldBroadcast: Boolean): ReadPreviousQueriesResponse = null

  override def runQuery(topicId: String, topicName:String, outputTypes: Set[ResultOutputType], queryDefinition: QueryDefinition, shouldBroadcast: Boolean): AggregatedRunQueryResponse = null

  override def readQueryInstances(queryId: Long, shouldBroadcast: Boolean): ReadQueryInstancesResponse = null

  override def readInstanceResults(instanceId: Long, shouldBroadcast: Boolean): AggregatedReadInstanceResultsResponse = null

  override def readPdo(patientSetCollId: String, optionsXml: NodeSeq, shouldBroadcast: Boolean): ReadPdoResponse = null

  override def readQueryDefinition(queryId: Long, shouldBroadcast: Boolean): ReadQueryDefinitionResponse = null

  override def deleteQuery(queryId: Long, shouldBroadcast: Boolean): DeleteQueryResponse = null

  override def renameQuery(queryId: Long, queryName: String, shouldBroadcast: Boolean): RenameQueryResponse = null

  override def readQueryResult(queryId: Long, shouldBroadcast: Boolean): AggregatedReadQueryResultResponse = null
  
  override def readTranslatedQueryDefinition(queryDef: QueryDefinition, shouldBroadcast: Boolean): AggregatedReadTranslatedQueryDefinitionResponse = null
  
  override def flagQuery(networkQueryId: Long, messageOption: Option[String], shouldBroadcast: Boolean): FlagQueryResponse = FlagQueryResponse
  
  override def unFlagQuery(networkQueryId: Long, shouldBroadcast: Boolean): UnFlagQueryResponse = UnFlagQueryResponse
}