package net.shrine.integration

import net.shrine.client.ShrineClient
import net.shrine.protocol.{AggregatedReadInstanceResultsResponse, AggregatedReadQueryResultResponse, AggregatedReadTranslatedQueryDefinitionResponse, AggregatedRunQueryResponse, AuthenticationInfo, DefaultBreakdownResultOutputTypes, DeleteQueryRequest, DeleteQueryResponse, FlagQueryRequest, FlagQueryResponse, ReadApprovedQueryTopicsResponse, ReadPdoResponse, ReadPreviousQueriesResponse, ReadQueryDefinitionResponse, ReadQueryInstancesResponse, RenameQueryResponse, ResultOutputType, RunQueryRequest, RunQueryResponse, UnFlagQueryRequest, UnFlagQueryResponse}
import net.shrine.util.XmlDateHelper
import net.shrine.client.Poster

import scala.xml.NodeSeq
import net.shrine.protocol.query.QueryDefinition
import net.shrine.client.HttpResponse
import net.shrine.crypto.TrustParam
import net.shrine.serialization.I2b2Marshaller

import scala.xml.Node
import scala.xml.XML
import net.shrine.util.XmlUtil

final case class I2b2ShrineClient(poster: Poster, projectId: String, authorization: AuthenticationInfo) extends ShrineClient {
  import scala.concurrent.duration._
  
  override def readApprovedQueryTopics(userId: String, shouldBroadcast: Boolean): ReadApprovedQueryTopicsResponse = ???

  override def readPreviousQueries(userId: String, fetchSize: Int, shouldBroadcast: Boolean): ReadPreviousQueriesResponse = ???

  override def runQuery(topicId: String, outputTypes: Set[ResultOutputType], queryDefinition: QueryDefinition, shouldBroadcast: Boolean): AggregatedRunQueryResponse = {
    val req = RunQueryRequest(projectId, 1.minute, authorization, None, None, outputTypes, queryDefinition)
    
    def stripWhitespace(xml: String): String = XmlUtil.stripWhitespace(XML.loadString(xml)).toString
    
    doSend(req, xml => AggregatedRunQueryResponse.fromI2b2String(DefaultBreakdownResultOutputTypes.toSet)(stripWhitespace(xml)).get)
  }
  
  override def readQueryInstances(queryId: Long, shouldBroadcast: Boolean): ReadQueryInstancesResponse = ???
  
  override def readInstanceResults(instanceId: Long, shouldBroadcast: Boolean): AggregatedReadInstanceResultsResponse = ???
  
  override def readPdo(patientSetCollId: String, optionsXml: NodeSeq, shouldBroadcast: Boolean): ReadPdoResponse = ???
  
  override def readQueryDefinition(queryId: Long, shouldBroadcast: Boolean): ReadQueryDefinitionResponse = ???
  
  override def deleteQuery(queryId: Long, shouldBroadcast: Boolean): DeleteQueryResponse = {
    val req = DeleteQueryRequest(projectId, 1.minute, authorization, queryId) 
    
    doSend(req, DeleteQueryResponse.fromI2b2)
  }
  
  override def renameQuery(queryId: Long, queryName: String, shouldBroadcast: Boolean): RenameQueryResponse = ???
  
  override def readTranslatedQueryDefinition(queryDef: QueryDefinition, shouldBroadcast: Boolean): AggregatedReadTranslatedQueryDefinitionResponse = ???
  
  override def flagQuery(networkQueryId: Long, message: Option[String], shouldBroadcast: Boolean): FlagQueryResponse = {
    val req = FlagQueryRequest(projectId, 1.minute, authorization, networkQueryId, message) 
    
    doSend(req, FlagQueryResponse.fromI2b2(_).get)
  }
  
  override def unFlagQuery(networkQueryId: Long, shouldBroadcast: Boolean): UnFlagQueryResponse = {
    val req = UnFlagQueryRequest(projectId, 1.minute, authorization, networkQueryId) 
    
    doSend(req, UnFlagQueryResponse.fromI2b2(_).get)
  }
  
  override def readQueryResult(queryId: Long, shouldBroadcast: Boolean): AggregatedReadQueryResultResponse = ???
  
  private def doSend[R](req: I2b2Marshaller, unmarshal: String => R): R = {
    val HttpResponse(_, i2b2Resp) = poster.post(req.toI2b2String)
    
    unmarshal(i2b2Resp)
  }
}