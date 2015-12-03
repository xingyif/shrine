package net.shrine.protocol

import scala.concurrent.duration.Duration
import scala.xml.NodeSeq
import net.shrine.serialization.I2b2Unmarshaller
import net.shrine.util.XmlUtil
import scala.util.Try
import net.shrine.util.NodeSeqEnrichments
import net.shrine.serialization.I2b2UnmarshallingHelpers

/**
 * @author Bill Simons
 * @date 3/9/11
 * @link http://cbmi.med.harvard.edu
 * @link http://chip.org
 *       <p/>
 *       NOTICE: This software comes with NO guarantees whatsoever and is
 *       licensed as Lgpl Open Source
 * @link http://www.gnu.org/licenses/lgpl.html
 *
 * NB: this is a case class to get a structural equality contract in hashCode and equals, mostly for testing
 */
final case class ReadApprovedQueryTopicsRequest(
  override val projectId: String,
  override val waitTime: Duration,
  override val authn: AuthenticationInfo,
  val userId: String) extends ShrineRequest(projectId, waitTime, authn) with HandleableShrineRequest with HandleableI2b2Request {

  override val requestType = RequestType.SheriffRequest

  //todo not used according to the IDE or override anything.
  override def handle(handler: ShrineRequestHandler, shouldBroadcast: Boolean) = handler.readApprovedQueryTopics(this, shouldBroadcast)

  override def handleI2b2(handler: I2b2RequestHandler, shouldBroadcast: Boolean) = handler.readApprovedQueryTopics(this, shouldBroadcast)

  override def toXml: NodeSeq = XmlUtil.stripWhitespace {
    <readApprovedQueryTopics>
      { headerFragment }
      <userId>{ userId }</userId>
    </readApprovedQueryTopics>
  }

  protected override def i2b2MessageBody: NodeSeq = XmlUtil.stripWhitespace {
    <message_body>
      <ns8:sheriff_header xsi:type="ns8:sheriffHeaderType" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
      <ns8:sheriff_request xsi:type="ns8:sheriffRequestType" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
    </message_body>
  }
}

object ReadApprovedQueryTopicsRequest extends I2b2XmlUnmarshaller[ReadApprovedQueryTopicsRequest] with ShrineXmlUnmarshaller[ReadApprovedQueryTopicsRequest] with ShrineRequestUnmarshaller with I2b2UnmarshallingHelpers {

  override def fromI2b2(breakdownTypes: Set[ResultOutputType])(xml: NodeSeq): Try[ReadApprovedQueryTopicsRequest] = {
    import NodeSeqEnrichments.Strictness._

    for {
      projectId <- i2b2ProjectId(xml)
      waitTime <- i2b2WaitTime(xml)
      authn <- i2b2AuthenticationInfo(xml)
      username <- (xml withChild "message_header" withChild "security" withChild "username").map(_.text)
    } yield {
      ReadApprovedQueryTopicsRequest(
        projectId,
        waitTime,
        authn,
        username)
    }
  }

  override def fromXml(breakdownTypes: Set[ResultOutputType])(xml: NodeSeq): Try[ReadApprovedQueryTopicsRequest] = {
    import NodeSeqEnrichments.Strictness._

    for {
      waitTime <- shrineWaitTime(xml)
      authn <- shrineAuthenticationInfo(xml)
      userId <- xml.withChild("userId").map(_.text)
      projectId <- shrineProjectId(xml)
    } yield {
      ReadApprovedQueryTopicsRequest(projectId, waitTime, authn, userId)
    }
  }
}