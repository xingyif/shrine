package net.shrine.integration

import net.shrine.client.ShrineClient
import net.shrine.protocol.AuthenticationInfo
import net.shrine.client.Poster
import net.shrine.protocol.ResultOutputType
import scala.xml.NodeSeq
import net.shrine.protocol.AggregatedReadTranslatedQueryDefinitionResponse
import net.shrine.protocol.ReadQueryDefinitionResponse
import net.shrine.protocol.UnFlagQueryResponse
import net.shrine.protocol.ReadPdoResponse
import net.shrine.protocol.RenameQueryResponse
import net.shrine.protocol.ReadQueryInstancesResponse
import net.shrine.protocol.query.QueryDefinition
import net.shrine.protocol.AggregatedRunQueryResponse
import net.shrine.protocol.FlagQueryResponse
import net.shrine.protocol.DeleteQueryResponse
import net.shrine.protocol.ReadPreviousQueriesResponse
import net.shrine.protocol.ReadApprovedQueryTopicsResponse
import net.shrine.protocol.AggregatedReadInstanceResultsResponse
import net.shrine.protocol.AggregatedReadQueryResultResponse
import net.shrine.protocol.DeleteQueryRequest
import net.shrine.client.HttpResponse
import net.shrine.protocol.FlagQueryRequest
import net.shrine.serialization.I2b2Marshaller
import net.shrine.protocol.UnFlagQueryRequest
import net.shrine.protocol.RunQueryRequest
import net.shrine.protocol.DefaultBreakdownResultOutputTypes
import scala.xml.XML
import net.shrine.util.XmlUtil

final case class I2b2ShrineClient(poster: Poster, projectId: String, authorization: AuthenticationInfo) extends ShrineClient {
  import scala.concurrent.duration._
  
  override def readApprovedQueryTopics(userId: String, shouldBroadcast: Boolean): ReadApprovedQueryTopicsResponse = ???

  override def readPreviousQueries(userId: String, fetchSize: Int, shouldBroadcast: Boolean): ReadPreviousQueriesResponse = ???

  override def runQuery(topicId: String, topicName:String, outputTypes: Set[ResultOutputType], queryDefinition: QueryDefinition, shouldBroadcast: Boolean): AggregatedRunQueryResponse = {
    val req = RunQueryRequest(projectId, 1.minute, authorization, -1, None, outputTypes, queryDefinition)
    
    def stripWhitespace(xml: String): String = XmlUtil.stripWhitespace(XML.loadString(xml)).toString()
    
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