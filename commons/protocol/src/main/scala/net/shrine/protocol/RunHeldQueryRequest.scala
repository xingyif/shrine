package net.shrine.protocol

import scala.concurrent.duration.Duration
import scala.xml.NodeSeq
import net.shrine.util.XmlUtil
import net.shrine.serialization.XmlUnmarshaller
import scala.util.Try
import net.shrine.serialization.I2b2Unmarshaller
import net.shrine.util.NodeSeqEnrichments
import net.shrine.serialization.I2b2UnmarshallingHelpers

/**
 * @author clint
 * @date Apr 30, 2014
 */
final case class RunHeldQueryRequest(
    override val projectId: String, 
    override val waitTime: Duration, 
    override val authn: AuthenticationInfo,
    networkQueryId: Long) extends ShrineRequest(projectId, waitTime, authn) with HandleableAdminShrineRequest {

  override val requestType = RequestType.RunHeldQueryRequest
  
  override def handleAdmin(handler: I2b2AdminRequestHandler, shouldBroadcast: Boolean): ShrineResponse = {
    handler.runHeldQuery(this, shouldBroadcast)
  }
  
  override def toXml: NodeSeq = XmlUtil.stripWhitespace {
    <runHeldQueryRequest>
      { headerFragment }
      <networkQueryId>{ networkQueryId }</networkQueryId>
    </runHeldQueryRequest>
  }
  
  override protected def i2b2MessageBody: NodeSeq = XmlUtil.stripWhitespace {
    <message_body>
      <runHeldQuery>
        <networkQueryId>{ networkQueryId }</networkQueryId>
      </runHeldQuery>
    </message_body>
  }
}

object RunHeldQueryRequest extends I2b2XmlUnmarshaller[RunHeldQueryRequest] with ShrineXmlUnmarshaller[RunHeldQueryRequest] with ShrineRequestUnmarshaller with I2b2UnmarshallingHelpers {
  override def fromXml(breakdownTypes: Set[ResultOutputType])(xml: NodeSeq): Try[RunHeldQueryRequest] = {
    import NodeSeqEnrichments.Strictness._
    
    for {
      RequestHeader(projectId, waitTime, authn) <- shrineHeader(xml)
      networkQueryId <- xml.withChild("networkQueryId").map(_.text.trim.toLong) 
    } yield {
      RunHeldQueryRequest(projectId, waitTime, authn, networkQueryId)
    }
  }
  
  override def fromI2b2(breakdownTypes: Set[ResultOutputType])(xml: NodeSeq): Try[RunHeldQueryRequest] = {
    import NodeSeqEnrichments.Strictness._
    
    for {
      RequestHeader(projectId, waitTime, authn) <- i2b2Header(xml)
      networkQueryId <- xml.withChild("message_body").withChild("runHeldQuery").withChild("networkQueryId").map(_.text.trim.toLong) 
    } yield {
      RunHeldQueryRequest(projectId, waitTime, authn, networkQueryId)
    }
  }
}