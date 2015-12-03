package net.shrine.protocol

import scala.util.Try
import net.shrine.util.XmlUtil
import scala.concurrent.duration.Duration
import scala.xml.NodeSeq
import net.shrine.util.NodeSeqEnrichments
import net.shrine.serialization.I2b2Unmarshaller
import net.shrine.serialization.I2b2UnmarshallingHelpers

/**
 * @author clint
 * @date Apr 18, 2014
 *
 * NB: Now needs to be serializable as an i2b2 blob, because even though flagging is a Shrine-only feature,
 * flagging will be initiated from the legacy i2b2 webclient for the forseeable future.  In that context,
 * generating i2b2 XML blobs and running them through the proxy is the path of least resistance. Sigh.
 * - 17 June 2014
 */
final case class UnFlagQueryRequest(
  override val projectId: String,
  override val waitTime: Duration,
  override val authn: AuthenticationInfo,
  networkQueryId: Long) extends ShrineRequest(projectId, waitTime, authn) with HandleableI2b2Request with HasHeaderFields {

  override val requestType = RequestType.UnFlagQueryRequest

  override def handleI2b2(handler: I2b2RequestHandler, shouldBroadcast: Boolean): ShrineResponse = {
    handler.unFlagQuery(this, shouldBroadcast)
  }

  import UnFlagQueryRequest.rootTagName

  override def toXml = XmlUtil.stripWhitespace {
    XmlUtil.renameRootTag(rootTagName) {
      <placeholder>
        { headerFragment }
        <networkQueryId>{ networkQueryId }</networkQueryId>
      </placeholder>
    }
  }

  protected override def i2b2MessageBody = XmlUtil.stripWhitespace {
    <message_body>
      { headerFragment }
      <ns4:request>
        {
          XmlUtil.renameRootTag(rootTagName) {
            <placeholder>
              <networkQueryId>{ networkQueryId }</networkQueryId>
            </placeholder>
          }
        }
      </ns4:request>
    </message_body>
  }
}

object UnFlagQueryRequest extends I2b2XmlUnmarshaller[UnFlagQueryRequest] with ShrineXmlUnmarshaller[UnFlagQueryRequest] with ShrineRequestUnmarshaller with I2b2UnmarshallingHelpers with HasRootTagName {

  override val rootTagName = "unFlagQuery"

  override def fromXml(breakdownTypes: Set[ResultOutputType])(xml: NodeSeq): Try[UnFlagQueryRequest] = {
    import NodeSeqEnrichments.Strictness._

    for {
      tagName <- Try(xml.head.label)
      if tagName == rootTagName
      projectId <- shrineProjectId(xml)
      waitTimeMs <- shrineWaitTime(xml)
      authn <- shrineAuthenticationInfo(xml)
      networkQueryId <- xml.withChild("networkQueryId").map(_.text.toLong)
    } yield {
      UnFlagQueryRequest(projectId, waitTimeMs, authn, networkQueryId)
    }
  }

  override def fromI2b2(breakdownTypes: Set[ResultOutputType])(xml: NodeSeq): Try[UnFlagQueryRequest] = {
    import NodeSeqEnrichments.Strictness._

    for {
      projectId <- i2b2ProjectId(xml)
      waitTime <- i2b2WaitTime(xml)
      authn <- i2b2AuthenticationInfo(xml)
      networkQueryId <- (xml withChild "message_body" withChild "request" withChild rootTagName withChild "networkQueryId").map(_.text.toLong)
    } yield {
      UnFlagQueryRequest(projectId, waitTime, authn, networkQueryId)
    }
  }
}