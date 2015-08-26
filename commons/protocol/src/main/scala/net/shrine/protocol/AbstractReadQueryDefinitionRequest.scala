package net.shrine.protocol

import scala.concurrent.duration.Duration
import net.shrine.util.XmlUtil
import net.shrine.serialization.I2b2Unmarshaller
import scala.xml.NodeSeq
import scala.util.Try
import net.shrine.util.NodeSeqEnrichments
import net.shrine.serialization.I2b2UnmarshallingHelpers

/**
 * @author clint
 * @date Feb 18, 2014
 */
abstract class AbstractReadQueryDefinitionRequest(
  override val projectId: String,
  override val waitTime: Duration,
  override val authn: AuthenticationInfo,
  val queryId: Long) extends ShrineRequest(projectId, waitTime, authn) with CrcRequest {

  override val requestType = RequestType.GetRequestXml

  override def toXml = XmlUtil.stripWhitespace {
    <readQueryDefinition>
      { headerFragment }
      <queryId>{ queryId }</queryId>
    </readQueryDefinition>
  }

  protected override def i2b2MessageBody = XmlUtil.stripWhitespace {
    <message_body>
      { i2b2PsmHeader }
      <ns4:request xsi:type="ns4:master_requestType" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
        <query_master_id>{ queryId }</query_master_id>
      </ns4:request>
    </message_body>
  }
}

object AbstractReadQueryDefinitionRequest {
  abstract class Companion[R <: AbstractReadQueryDefinitionRequest](makeRequest: (String, Duration, AuthenticationInfo, Long) => R) extends I2b2XmlUnmarshaller[R] with ShrineXmlUnmarshaller[R] with ShrineRequestUnmarshaller with I2b2UnmarshallingHelpers {

    override def fromI2b2(breakdownTypes: Set[ResultOutputType])(xml: NodeSeq): Try[R] = {
      import NodeSeqEnrichments.Strictness._
      for {
        projectId <- i2b2ProjectId(xml)
        waitTime <- i2b2WaitTime(xml)
        authn <- i2b2AuthenticationInfo(xml)
        masterId <- (xml withChild "message_body" withChild "request" withChild "query_master_id").map(_.text.toLong)
      } yield {
        makeRequest(
          projectId,
          waitTime,
          authn,
          masterId)
      }
    }

    override def fromXml(breakdownTypes: Set[ResultOutputType])(xml: NodeSeq): Try[R] = {
      import NodeSeqEnrichments.Strictness._

      for {
        waitTimeMs <- shrineWaitTime(xml)
        authn <- shrineAuthenticationInfo(xml)
        queryId <- xml.withChild("queryId").map(_.text.toLong)
        projectId <- shrineProjectId(xml)
      } yield {
        makeRequest(projectId, waitTimeMs, authn, queryId)
      }
    }
  }
}