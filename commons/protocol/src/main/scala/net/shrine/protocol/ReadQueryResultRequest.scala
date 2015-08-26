package net.shrine.protocol

import scala.concurrent.duration.Duration
import scala.util.Try
import scala.xml.NodeSeq
import net.shrine.util.XmlUtil
import net.shrine.util.NodeSeqEnrichments

/**
 * @author clint
 * @date Nov 2, 2012
 */
final case class ReadQueryResultRequest(
    val projectId: String, //TODO: needed?
    val waitTime: Duration,  //TODO: needed?
    val authn: AuthenticationInfo, //TODO: needed?
    queryId: Long) extends NonI2b2ShrineRequest {
  
  override val requestType = RequestType.GetQueryResult 
  
  override def toXml: NodeSeq = XmlUtil.stripWhitespace(
    <readQueryResult>
      <projectId>{ projectId }</projectId>
      <waitTimeMs>{ waitTime.toMillis }</waitTimeMs>
      { authn.toXml }
      <queryId>{ queryId }</queryId>
    </readQueryResult>)
}

object ReadQueryResultRequest extends ShrineXmlUnmarshaller[ReadQueryResultRequest] with ShrineRequestUnmarshaller {
  override def fromXml(breakdownTypes: Set[ResultOutputType])(xml: NodeSeq): Try[ReadQueryResultRequest] = {
    import NodeSeqEnrichments.Strictness._
    
    for {
      waitTime <- shrineWaitTime(xml)
      authn <- shrineAuthenticationInfo(xml)
      queryId <- xml.withChild("queryId").map(_.text.toLong)
      projectId <- shrineProjectId(xml)
    } yield {
      ReadQueryResultRequest(projectId, waitTime, authn, queryId)
    }
  }
}