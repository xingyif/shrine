package net.shrine.protocol

import scala.concurrent.duration.Duration
import scala.util.{Failure, Success, Try}
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
final case class DeleteQueryRequest(
                                     override val projectId: String,
                                     override val waitTime: Duration,
                                     override val authn: AuthenticationInfo,
                                     networkQueryId: Long) extends ShrineRequest(projectId, waitTime, authn) with CrcRequest with TranslatableRequest[DeleteQueryRequest] with HandleableShrineRequest with HandleableI2b2Request {

  override val requestType = RequestType.MasterDeleteRequest

  override def handle(handler: ShrineRequestHandler, shouldBroadcast: Boolean) = handler.deleteQuery(this, shouldBroadcast)

  override def handleI2b2(handler: I2b2RequestHandler, shouldBroadcast: Boolean) = handler.deleteQuery(this, shouldBroadcast)

  override def toXml: NodeSeq = XmlUtil.stripWhitespace {
    <deleteQuery>
      { headerFragment }
      <queryId>{ networkQueryId }</queryId>
    </deleteQuery>
  }

  def withId(id: Long) = this.copy(networkQueryId = id)

  override def withAuthn(ai: AuthenticationInfo) = this.copy(authn = ai)

  override def withProject(proj: String) = this.copy(projectId = proj)

  protected override def i2b2MessageBody = XmlUtil.stripWhitespace {
    <message_body>
      { i2b2PsmHeader }
      <ns4:request xsi:type="ns4:master_delete_requestType" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
        <user_id>{ authn.username }</user_id>
        <query_master_id>{ networkQueryId }</query_master_id>
      </ns4:request>
    </message_body>
  }
}

object DeleteQueryRequest extends I2b2XmlUnmarshaller[DeleteQueryRequest] with ShrineXmlUnmarshaller[DeleteQueryRequest] with ShrineRequestUnmarshaller with I2b2UnmarshallingHelpers {

  override def fromI2b2(breakdownTypes: Set[ResultOutputType])(xml: NodeSeq): Try[DeleteQueryRequest] = {
    import NodeSeqEnrichments.Strictness._

    for {
      projectId <- i2b2ProjectId(xml)
      waitTime <- i2b2WaitTime(xml)
      authn <- i2b2AuthenticationInfo(xml)
      masterIdString <- (xml withChild "message_body" withChild "request" withChild "query_master_id").map(_.text)
      masterId <- Try(masterIdString.toLong).transform(id => Success(id),
        x => Failure(I2B2MessageFormatException(s"query_master_id of $masterIdString could not be interpreted as a Long integer",x)))
    } yield {
      DeleteQueryRequest(projectId, waitTime, authn, masterId)
    }
  }

  override def fromXml(breakdownTypes: Set[ResultOutputType])(xml: NodeSeq): Try[DeleteQueryRequest] = {
    import NodeSeqEnrichments.Strictness._
    
    for {
      waitTime <- shrineWaitTime(xml)
      authn <- shrineAuthenticationInfo(xml)
      queryId <- xml.withChild("queryId").map(_.text.toLong)
      projectId <- shrineProjectId(xml)
    } yield {
      DeleteQueryRequest(projectId, waitTime, authn, queryId)
    }
  }
}

case class I2B2MessageFormatException(message:String,cause:Throwable) extends Exception(message,cause)