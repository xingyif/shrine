package net.shrine.client

import scala.xml.NodeSeq
import net.shrine.protocol.AggregatedReadInstanceResultsResponse
import net.shrine.protocol.AggregatedReadQueryResultResponse
import net.shrine.protocol.AggregatedRunQueryResponse
import net.shrine.protocol.DeleteQueryResponse
import net.shrine.protocol.ReadApprovedQueryTopicsResponse
import net.shrine.protocol.ReadPdoResponse
import net.shrine.protocol.ReadPreviousQueriesResponse
import net.shrine.protocol.ReadQueryDefinitionResponse
import net.shrine.protocol.ReadQueryInstancesResponse
import net.shrine.protocol.RenameQueryResponse
import net.shrine.protocol.ResultOutputType
import net.shrine.protocol.query.QueryDefinition
import net.shrine.protocol.AggregatedReadTranslatedQueryDefinitionResponse
import net.shrine.protocol.FlagQueryResponse
import net.shrine.protocol.UnFlagQueryResponse

/**
 *
 * @author Clint Gilbert
 * @since Sep 14, 2011
 *
 * @see http://cbmi.med.harvard.edu
 *
 */
trait ShrineClient {
  def readApprovedQueryTopics(userId: String, shouldBroadcast: Boolean): ReadApprovedQueryTopicsResponse

  def readPreviousQueries(userId: String, fetchSize: Int, shouldBroadcast: Boolean): ReadPreviousQueriesResponse

  def runQuery(topicId: String, topicName:String, outputTypes: Set[ResultOutputType], queryDefinition: QueryDefinition, shouldBroadcast: Boolean): AggregatedRunQueryResponse
  
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

  def runQuery(topicId: String, topicName:String, outputTypes: java.util.Set[ResultOutputType], queryDefinition: QueryDefinition, shouldBroadcast: Boolean): AggregatedRunQueryResponse = runQuery(topicId, topicName, outputTypes.asScala.toSet, queryDefinition, shouldBroadcast)
}