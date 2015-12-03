package net.shrine.protocol

import scala.concurrent.duration.Duration
import scala.util.Try
import scala.xml.NodeSeq
import net.shrine.serialization.I2b2Unmarshaller
import net.shrine.util.XmlUtil
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
final case class ReadPreviousQueriesRequest(
  override val projectId: String,
  override val waitTime: Duration,
  override val authn: AuthenticationInfo,
  val userId: String,
  val fetchSize: Int) extends ShrineRequest(projectId, waitTime, authn) with CrcRequest with HandleableShrineRequest with HandleableI2b2Request {

  override val requestType = RequestType.UserRequest

  override def handle(handler: ShrineRequestHandler, shouldBroadcast: Boolean) = handler.readPreviousQueries(this, shouldBroadcast)

  override def handleI2b2(handler: I2b2RequestHandler, shouldBroadcast: Boolean) = handler.readPreviousQueries(this, shouldBroadcast)

  override def toXml: NodeSeq = XmlUtil.stripWhitespace {
    <readPreviousQueries>
      { headerFragment }
      <userId>{ userId }</userId>
      <fetchSize>{ fetchSize }</fetchSize>
    </readPreviousQueries>
  }

  protected override def i2b2MessageBody: NodeSeq = XmlUtil.stripWhitespace {
    <message_body>
      { i2b2PsmHeader }
      <ns4:request xsi:type="ns4:user_requestType" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
        <user_id>{ userId }</user_id>
        <group_id>{ projectId }</group_id>
        <fetch_size>{ fetchSize }</fetch_size>
      </ns4:request>
    </message_body>
  }
}

object ReadPreviousQueriesRequest extends I2b2XmlUnmarshaller[ReadPreviousQueriesRequest] with ShrineXmlUnmarshaller[ReadPreviousQueriesRequest] with ShrineRequestUnmarshaller with I2b2UnmarshallingHelpers {

  override def fromI2b2(breakdownTypes: Set[ResultOutputType])(nodeSeq: NodeSeq): Try[ReadPreviousQueriesRequest] = {
    import NodeSeqEnrichments.Strictness._

    for {
      projectId <- i2b2ProjectId(nodeSeq)
      waitTime <- i2b2WaitTime(nodeSeq)
      authn <- i2b2AuthenticationInfo(nodeSeq)
      userId <- (nodeSeq withChild "message_body" withChild "request" withChild "user_id").map(_.text)
      fetchSize <- (nodeSeq withChild "message_body" withChild "request" withChild "fetch_size").map(_.text.toInt)
    } yield {
      ReadPreviousQueriesRequest(
        projectId,
        waitTime,
        authn,
        userId,
        fetchSize)
    }
  }

  override def fromXml(breakdownTypes: Set[ResultOutputType])(xml: NodeSeq): Try[ReadPreviousQueriesRequest] = {
    import NodeSeqEnrichments.Strictness._

    for {
      waitTime <- shrineWaitTime(xml)
      authn <- shrineAuthenticationInfo(xml)
      fetchSize <- xml.withChild("fetchSize").map(_.text.toInt)
      userId <- xml.withChild("userId").map(_.text)
      projectId <- shrineProjectId(xml)
    } yield {
      ReadPreviousQueriesRequest(projectId, waitTime, authn, userId, fetchSize)
    }
  }
}