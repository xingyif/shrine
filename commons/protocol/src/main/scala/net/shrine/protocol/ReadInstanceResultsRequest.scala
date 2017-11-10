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
 * @date 3/17/11
 * @link http://cbmi.med.harvard.edu
 * @link http://chip.org
 *       <p/>
 *       NOTICE: This software comes with NO guarantees whatsoever and is
 *       licensed as Lgpl Open Source
 * @link http://www.gnu.org/licenses/lgpl.html
 *
 * NB: this is a case class to get a structural equality contract in hashCode and equals, mostly for testing
 *
 * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
 * NOTE: Now that the adapter caches/stores results from the CRC, Instead of an
 * i2b2 instance id, this class now contains the Shrine-generated, network-wide
 * id of a query, which is used to obtain results previously obtained from the
 * CRC from Shrine's datastore.
 * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
 */
final case class ReadInstanceResultsRequest(
  override val projectId: String,
  override val waitTime: Duration,
  override val authn: AuthenticationInfo,
  /*
   * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
   * NOTE: Now that the adapter caches/stores results from the CRC, Instead of an
   * i2b2 instance id, this class now contains the Shrine-generated, network-wide 
   * id of a query, which is used to obtain results previously obtained from the 
   * CRC from Shrine's datastore.
   * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
   *
   * But of course if you're using it to make a call to the CRC you need to use the query instant id from the CRC, not the network id.
   */
  shrineNetworkQueryId: Long) extends ShrineRequest(projectId, waitTime, authn) with CrcRequest with HandleableShrineRequest with HandleableI2b2Request {

  override val requestType = RequestType.InstanceRequest

  override def handle(handler: ShrineRequestHandler, shouldBroadcast: Boolean) = handler.readInstanceResults(this, shouldBroadcast)

  override def handleI2b2(handler: I2b2RequestHandler, shouldBroadcast: Boolean) = handler.readInstanceResults(this, shouldBroadcast)

  override def toXml = XmlUtil.stripWhitespace {
    <readInstanceResults>
      { headerFragment }
      <shrineNetworkQueryId>{ shrineNetworkQueryId }</shrineNetworkQueryId>
    </readInstanceResults>
  }

  def withId(id: Long) = this.copy(shrineNetworkQueryId = id)

  def withProject(proj: String) = this.copy(projectId = proj)

  def withAuthn(ai: AuthenticationInfo) = this.copy(authn = ai)

  protected override def i2b2MessageBody = XmlUtil.stripWhitespace {
    <message_body>
      { i2b2PsmHeader }
      <ns4:request xsi:type="ns4:instance_requestType" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
        <query_instance_id>{ shrineNetworkQueryId }</query_instance_id>
      </ns4:request>
    </message_body>
  }
}

object ReadInstanceResultsRequest extends I2b2XmlUnmarshaller[ReadInstanceResultsRequest] with ShrineXmlUnmarshaller[ReadInstanceResultsRequest] with ShrineRequestUnmarshaller with I2b2UnmarshallingHelpers {

  override def fromI2b2(breakdownTypes: Set[ResultOutputType])(nodeSeq: NodeSeq): Try[ReadInstanceResultsRequest] = {
    import NodeSeqEnrichments.Strictness._

    for {
      projectId <- i2b2ProjectId(nodeSeq)
      waitTime <- i2b2WaitTime(nodeSeq)
      authn <- i2b2AuthenticationInfo(nodeSeq)
      instanceId <- (nodeSeq withChild "message_body" withChild "request" withChild "query_instance_id").map(_.text.toLong)
    } yield {
      ReadInstanceResultsRequest(
        projectId,
        waitTime,
        authn,
        instanceId)
    }
  }

  override def fromXml(breakdownTypes: Set[ResultOutputType])(xml: NodeSeq): Try[ReadInstanceResultsRequest] = {
    import NodeSeqEnrichments.Strictness._

    for {
      waitTime <- shrineWaitTime(xml)
      authn <- shrineAuthenticationInfo(xml)
      shrineNetworkQueryIdXml <- xml withChild "shrineNetworkQueryId"
      shrineNetworkQueryId <- Try(shrineNetworkQueryIdXml.text.toLong)
      projectId <- shrineProjectId(xml)
    } yield {
      ReadInstanceResultsRequest(projectId, waitTime, authn, shrineNetworkQueryId)
    }
  }
}