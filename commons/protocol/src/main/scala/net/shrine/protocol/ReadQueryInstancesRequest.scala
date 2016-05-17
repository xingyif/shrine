package net.shrine.protocol

import xml.NodeSeq
import net.shrine.util.XmlUtil
import scala.util.Try
import scala.concurrent.duration.Duration
import net.shrine.util.NodeSeqEnrichments
import net.shrine.serialization.I2b2UnmarshallingHelpers

/**
 * @author Bill Simons
 * @since 3/17/11
 * @see http://cbmi.med.harvard.edu
 * @see http://chip.org
 *       <p/>
 *       NOTICE: This software comes with NO guarantees whatsoever and is
 *       licensed as Lgpl Open Source
 * @see http://www.gnu.org/licenses/lgpl.html
 *
 * NB: this is a case class to get a structural equality contract in hashCode and equals, mostly for testing
 */
final case class ReadQueryInstancesRequest(
                                            override val projectId: String,
                                            override val waitTime: Duration,
                                            override val authn: AuthenticationInfo,
                                            networkQueryId: Long) extends ShrineRequest(projectId, waitTime, authn) with CrcRequest with HandleableShrineRequest with HandleableI2b2Request {

  override val requestType = RequestType.MasterRequest

  override def handle(handler: ShrineRequestHandler, shouldBroadcast: Boolean) = handler.readQueryInstances(this, shouldBroadcast)

  override def handleI2b2(handler: I2b2RequestHandler, shouldBroadcast: Boolean) = handler.readQueryInstances(this, shouldBroadcast)

  override def toXml = XmlUtil.stripWhitespace {
    <readQueryInstances>
      { headerFragment }
      <queryId>{ networkQueryId }</queryId>
    </readQueryInstances>
  }

  protected override def i2b2MessageBody = XmlUtil.stripWhitespace {
    <message_body>
      { i2b2PsmHeader }
      <ns4:request xsi:type="ns4:master_requestType" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
        <query_master_id>{ networkQueryId }</query_master_id>
      </ns4:request>
    </message_body>
  }
}

object ReadQueryInstancesRequest extends I2b2XmlUnmarshaller[ReadQueryInstancesRequest] with ShrineXmlUnmarshaller[ReadQueryInstancesRequest] with ShrineRequestUnmarshaller with I2b2UnmarshallingHelpers {

  override def fromI2b2(breakdownTypes: Set[ResultOutputType])(nodeSeq: NodeSeq): Try[ReadQueryInstancesRequest] = {
    import NodeSeqEnrichments.Strictness._

    for {
      projectId <- i2b2ProjectId(nodeSeq)
      waitTime <- i2b2WaitTime(nodeSeq)
      authn <- i2b2AuthenticationInfo(nodeSeq)
      masterId <- (nodeSeq withChild "message_body" withChild "request" withChild "query_master_id").map(_.text.toLong)
    } yield {
      ReadQueryInstancesRequest(
        projectId,
        waitTime,
        authn,
        masterId)
    }
  }

  override def fromXml(breakdownTypes: Set[ResultOutputType])(xml: NodeSeq): Try[ReadQueryInstancesRequest] = {
    import NodeSeqEnrichments.Strictness._

    for {
      waitTime <- shrineWaitTime(xml)
      authn <- shrineAuthenticationInfo(xml)
      queryId <- xml.withChild("queryId").map(_.text.toLong)
      projectId <- shrineProjectId(xml)
    } yield {
      ReadQueryInstancesRequest(projectId, waitTime, authn, queryId)
    }
  }
}