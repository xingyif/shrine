package net.shrine.client

import scala.xml.NodeSeq
import net.shrine.protocol.{AggregatedReadInstanceResultsResponse, AggregatedReadQueryResultResponse, AggregatedReadTranslatedQueryDefinitionResponse, AggregatedRunQueryResponse, DeleteQueryResponse, FlagQueryResponse, ReadApprovedQueryTopicsResponse, ReadPdoResponse, ReadPreviousQueriesResponse, ReadQueryDefinitionResponse, ReadQueryInstancesResponse, RenameQueryResponse, ResultOutputType, UnFlagQueryResponse}
import net.shrine.protocol.query.QueryDefinition

/**
 *
 * @author Clint Gilbert
 * @date Sep 14, 2011
 *
 * @link http://cbmi.med.harvard.edu
 *
 */
trait ShrineClient {
  def readApprovedQueryTopics(userId: String, shouldBroadcast: Boolean): ReadApprovedQueryTopicsResponse

  def readPreviousQueries(userId: String, fetchSize: Int, shouldBroadcast: Boolean): ReadPreviousQueriesResponse

  def runQuery(topicId: String, outputTypes: Set[ResultOutputType], queryDefinition: QueryDefinition, shouldBroadcast: Boolean): AggregatedRunQueryResponse
  
  def readQueryInstances(queryId: Long, shouldBroadcast: Boolean): ReadQueryInstancesResponse
  
  def readInstanceResults(instanceId: Long, shouldBroadcast: Boolean): AggregatedReadInstanceResultsResponse
  
  def readPdo(patientSetCollId: String, optionsXml: NodeSeq, shouldBroadcast: Boolean): ReadPdoResponse
  
  def readQueryDefinition(queryId: Long, shouldBroadcast: Boolean): ReadQueryDefinitionResponse
  
  def deleteQuery(queryId: Long, shouldBroadcast: Boolean): DeleteQueryResponse
  
  def renameQuery(queryId: Long, queryName: String, shouldBroadcast: Boolean): RenameQueryResponse
  
  def readTranslatedQueryDefinition(queryDef: QueryDefinition, shouldBroadcast: Boolean): AggregatedReadTranslatedQueryDefinitionResponse
  
  def flagQuery(networkQueryId: Long, message: Option[String], shouldBroadcast: Boolean): FlagQueryResponse
  
  def unFlagQuery(networkQueryId: Long, shouldBroadcast: Boolean): UnFlagQueryResponse
  
  def readQueryResult(queryId: Long, shouldBroadcast: Boolean): AggregatedReadQueryResultResponse
  
  //Overloads for Java interop
  import scala.collection.JavaConverters._

  def runQuery(topicId: String, outputTypes: java.util.Set[ResultOutputType], queryDefinition: QueryDefinition, shouldBroadcast: Boolean): AggregatedRunQueryResponse = runQuery(topicId, outputTypes.asScala.toSet, queryDefinition, shouldBroadcast)
}