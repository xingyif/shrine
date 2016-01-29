package net.shrine.protocol

import net.shrine.log.Loggable

import scala.concurrent.duration.Duration
import scala.util.Try
import scala.xml.NodeSeq
import net.shrine.serialization.I2b2Unmarshaller
import net.shrine.util.XmlUtil
import net.shrine.util.NodeSeqEnrichments
import net.shrine.util.XmlUtil
import scala.xml.NodeBuffer
import net.shrine.serialization.I2b2Unmarshaller
import net.shrine.serialization.I2b2UnmarshallingHelpers

/**
 * @author Clint Gilbert
 * @date March, 2014
 *
 * NB: Now needs to be serializable as an i2b2 blob, because even though flagging is a Shrine-only feature,
 * flagging will be initiated from the legacy i2b2 webclient for the forseeable future.  In that context,
 * generating i2b2 XML blobs and running them through the proxy is the path of least resistance. Sigh.
 * - 17 June 2014
 */
final case class FlagQueryRequest(
  override val projectId: String,
  override val waitTime: Duration,
  override val authn: AuthenticationInfo,
  networkQueryId: Long,
  message: Option[String]) extends ShrineRequest(projectId, waitTime, authn) with HandleableI2b2Request with HasHeaderFields {

  override val requestType = RequestType.FlagQueryRequest

  override def handleI2b2(handler: I2b2RequestHandler, shouldBroadcast: Boolean): ShrineResponse = {
    handler.flagQuery(this, shouldBroadcast)
  }

  import FlagQueryRequest.rootTagName

  override def toXml = XmlUtil.stripWhitespace {
    XmlUtil.renameRootTag(rootTagName) {
      <placeholder>
        { headerFragment }
        <networkQueryId>{ networkQueryId }</networkQueryId>
        { message.map(m => <message>{ m }</message>).orNull }
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
              { message.map(m => <message>{ m }</message>).orNull }
            </placeholder>
          }
        }
      </ns4:request>
    </message_body>
  }
}

object FlagQueryRequest extends I2b2XmlUnmarshaller[FlagQueryRequest] with ShrineXmlUnmarshaller[FlagQueryRequest] with ShrineRequestUnmarshaller with I2b2UnmarshallingHelpers with HasRootTagName with Loggable {

  override val rootTagName = "flagQuery"

  override def fromXml(breakdownTypes: Set[ResultOutputType])(xml: NodeSeq): Try[FlagQueryRequest] = {
    import NodeSeqEnrichments.Strictness._

    for {
      tagName <- Try(xml.head.label)
      if tagName == rootTagName
      projectId <- shrineProjectId(xml)
      waitTimeMs <- shrineWaitTime(xml)
      authn <- shrineAuthenticationInfo(xml)
      networkQueryId <- xml.withChild("networkQueryId").map(_.text.toLong)
      message = (xml \ "message").headOption.map(_.text)
    } yield {
      info(s"FlagQueryRequest fromXML $message from $xml")

      FlagQueryRequest(projectId, waitTimeMs, authn, networkQueryId, message)
    }
  }

  override def fromI2b2(breakdownTypes: Set[ResultOutputType])(xml: NodeSeq): Try[FlagQueryRequest] = {
    import NodeSeqEnrichments.Strictness._

    for {
      projectId <- i2b2ProjectId(xml)
      waitTime <- i2b2WaitTime(xml)
      authn <- i2b2AuthenticationInfo(xml)
      networkQueryId <- (xml withChild "message_body" withChild "request" withChild rootTagName withChild "networkQueryId").map(_.text.toLong)
      message = (xml \ "message_body" \ "request" \ rootTagName \ "message").headOption.map(_.text)
    } yield {
      info(s"FlagQueryRequest fromI2b2 $message from $xml")

      FlagQueryRequest(projectId, waitTime, authn, networkQueryId, message)
    }
  }
}