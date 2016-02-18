package net.shrine.protocol

import scala.concurrent.duration.Duration
import scala.util.Try
import scala.xml.NodeSeq
import net.shrine.util.XmlUtil
import net.shrine.util.NodeSeqEnrichments
import net.shrine.serialization.I2b2UnmarshallingHelpers

/**
 * @author Bill Simons
 * @since 3/28/11
 * @see http://cbmi.med.harvard.edu
 * @see http://chip.org
 *       <p/>
 *       NOTICE: This software comes with NO guarantees whatsoever and is
 *       licensed as Lgpl Open Source
 * @see http://www.gnu.org/licenses/lgpl.html
 *
 * NB: this is a case class to get a structural equality contract in hashCode and equals, mostly for testing
 */
final case class RenameQueryRequest(
                                     override val projectId: String,
                                     override val waitTime: Duration,
                                     override val authn: AuthenticationInfo,
                                     networkQueryId: Long,
                                     queryName: String) extends ShrineRequest(projectId, waitTime, authn) with CrcRequest with TranslatableRequest[RenameQueryRequest] with HandleableShrineRequest with HandleableI2b2Request {

  override val requestType = RequestType.MasterRenameRequest

  override def handle(handler: ShrineRequestHandler, shouldBroadcast: Boolean) = handler.renameQuery(this, shouldBroadcast)

  override def handleI2b2(handler: I2b2RequestHandler, shouldBroadcast: Boolean) = handler.renameQuery(this, shouldBroadcast)

  override def toXml = XmlUtil.stripWhitespace {
    <renameQuery>
      { headerFragment }
      <queryId>{ networkQueryId }</queryId>
      <queryName>{ queryName }</queryName>
    </renameQuery>
  }

  override def withAuthn(ai: AuthenticationInfo) = this.copy(authn = ai)

  override def withProject(proj: String) = this.copy(projectId = proj)

  protected override def i2b2MessageBody = XmlUtil.stripWhitespace {
    <message_body>
      { i2b2PsmHeader }
      <ns4:request xsi:type="ns4:master_rename_requestType" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
        <user_id>{ authn.username }</user_id>
        <query_master_id>{ networkQueryId }</query_master_id>
        <query_name>{ queryName }</query_name>
      </ns4:request>
    </message_body>
  }
}

object RenameQueryRequest extends I2b2XmlUnmarshaller[RenameQueryRequest] with ShrineXmlUnmarshaller[RenameQueryRequest] with ShrineRequestUnmarshaller with I2b2UnmarshallingHelpers {

  override def fromI2b2(breakdownTypes: Set[ResultOutputType])(nodeSeq: NodeSeq): Try[RenameQueryRequest] = {
    import NodeSeqEnrichments.Strictness._

    for {
      projectId <- i2b2ProjectId(nodeSeq)
      waitTime <- i2b2WaitTime(nodeSeq)
      authn <- i2b2AuthenticationInfo(nodeSeq)
      masterId <- (nodeSeq withChild "message_body" withChild "request" withChild "query_master_id").map(_.text.toLong)
      queryName <- (nodeSeq withChild "message_body" withChild "request" withChild "query_name").map(_.text)
    } yield {
      RenameQueryRequest(projectId, waitTime, authn, masterId, queryName)
    }
  }

  override def fromXml(breakdownTypes: Set[ResultOutputType])(xml: NodeSeq): Try[RenameQueryRequest] = {
    import NodeSeqEnrichments.Strictness._

    for {
      waitTimeMs <- shrineWaitTime(xml)
      authn <- shrineAuthenticationInfo(xml)
      queryId <- xml.withChild("queryId").map(_.text.toLong)
      projectId <- shrineProjectId(xml)
      queryName <- xml.withChild("queryName").map(_.text)
    } yield {
      RenameQueryRequest(projectId, waitTimeMs, authn, queryId, queryName)
    }
  }
}