package net.shrine.adapter.service

import scala.util.Try
import scala.xml.NodeSeq
import net.shrine.client.HttpClient
import net.shrine.protocol.ErrorResponse
import net.shrine.protocol.ReadI2b2AdminPreviousQueriesRequest
import net.shrine.protocol.ReadPreviousQueriesResponse
import net.shrine.protocol.ReadQueryDefinitionRequest
import net.shrine.protocol.ReadQueryDefinitionResponse
import net.shrine.protocol.ShrineResponse
import net.shrine.serialization.I2b2Marshaller
import net.shrine.serialization.I2b2Unmarshaller
import net.shrine.protocol.ReadI2b2AdminQueryingUsersRequest
import net.shrine.protocol.ReadI2b2AdminQueryingUsersResponse
import net.shrine.protocol.I2b2AdminReadQueryDefinitionRequest
import net.shrine.protocol.RunHeldQueryRequest
import net.shrine.protocol.RunQueryResponse
import net.shrine.protocol.ResultOutputType
import scala.xml.XML
import net.shrine.protocol.DefaultBreakdownResultOutputTypes

/**
 * @author clint
 * @date Apr 10, 2013
 */
final case class I2b2AdminClient(url: String, httpClient: HttpClient, breakdownTypes: Set[ResultOutputType] = DefaultBreakdownResultOutputTypes.toSet) {
  import I2b2AdminClient._
  
  def readI2b2AdminPreviousQueries(request: ReadI2b2AdminPreviousQueriesRequest): ShrineResponse = {
    doCall(request, (ReadPreviousQueriesResponse.fromI2b2 _) orElse (ErrorResponse.fromI2b2 _))
  }
  
  def readI2b2AdminQueryingUsers(request: ReadI2b2AdminQueryingUsersRequest): ShrineResponse = {
    doCall(request, (ReadI2b2AdminQueryingUsersResponse.fromI2b2 _) orElse (ErrorResponse.fromI2b2 _))
  }

  def readQueryDefinition(request: I2b2AdminReadQueryDefinitionRequest): ShrineResponse = {
    doCall(request, (ReadQueryDefinitionResponse.fromI2b2 _) orElse (ErrorResponse.fromI2b2 _))
  }

  def runHeldQuery(request: RunHeldQueryRequest): ShrineResponse = {
    doCall(request, (RunQueryResponse.fromI2b2 _) orElse (ErrorResponse.fromI2b2 _))
  }
  
  private def doCall(request: I2b2Marshaller, unmarshaller: I2b2AdminClient.Unmarshaller[ShrineResponse]): ShrineResponse = {
    val responseXml = XML.loadString(httpClient.post(request.toI2b2String, url).body)
    
    unmarshaller(breakdownTypes)(responseXml)
  }
}

object I2b2AdminClient {
  type Unmarshaller[R] = Set[ResultOutputType] => NodeSeq => R
  
  type TryUnmarshaller[R] = Set[ResultOutputType] => NodeSeq => Try[R]
  
  private def fallsBackI2b2Unmarshaller[R <: ShrineResponse, S <: ShrineResponse](lhs: Unmarshaller[R], rhs: Unmarshaller[S]): Unmarshaller[ShrineResponse] = {
    breakdownTypes => xml => Try(lhs(breakdownTypes)(xml)).getOrElse(rhs(breakdownTypes)(xml))
  } 
  
  private implicit class HasOrElseComplexUnmarshaller[R <: ShrineResponse](val lhs: Unmarshaller[R]) extends AnyVal {
    def orElse[S <: ShrineResponse](rhs: NodeSeq => S)(implicit discriminator: Int = 42): Unmarshaller[ShrineResponse] = fallsBackI2b2Unmarshaller(lhs, _ => rhs)
  }
  
  private implicit class HasOrElseTryUnmarshaller[R <: ShrineResponse](val lhs: TryUnmarshaller[R]) extends AnyVal {
    def orElse[S <: ShrineResponse](rhs: NodeSeq => S)(implicit discriminator: Int = 42): Unmarshaller[ShrineResponse] = fallsBackI2b2Unmarshaller(breakdownTypes => xml => lhs(breakdownTypes)(xml).get, _ => rhs)
  }
  
  private implicit class HasOrElseSimpleUnmarshaller[R <: ShrineResponse](val lhs: NodeSeq => R) extends AnyVal {
    def orElse[S <: ShrineResponse](rhs: NodeSeq => S)(implicit discriminator: Int = 42): Unmarshaller[ShrineResponse] = fallsBackI2b2Unmarshaller(_ => lhs, _ => rhs)
  }
}