package net.shrine.protocol

import scala.xml.NodeSeq
import net.shrine.util.XmlUtil
import net.shrine.serialization.I2b2Unmarshaller
import net.shrine.protocol.handlers.ReadQueryDefinitionHandler
import scala.util.Try
import scala.concurrent.duration.Duration

/**
 * @author Bill Simons
 * @since 3/9/11
 * @see http://cbmi.med.harvard.edu
 * @see http://chip.org
 *       <p/>
 *       NOTICE: This software comes with NO guarantees whatsoever and is
 *       licensed as Lgpl Open Source
 * @see http://www.gnu.org/licenses/lgpl.html
 *
 * NB: this is a case class to get a structural equality contract in hashCode and equals, mostly for testing
 */
final case class ReadQueryDefinitionRequest(
  override val projectId: String,
  override val waitTime: Duration,
  override val authn: AuthenticationInfo,
  override val queryId: Long) extends AbstractReadQueryDefinitionRequest(projectId, waitTime, authn, queryId) with HandleableShrineRequest with HandleableI2b2Request {

  override val requestType = RequestType.GetRequestXml

  override def handle(handler: ShrineRequestHandler, shouldBroadcast: Boolean) = handler.readQueryDefinition(this, shouldBroadcast)
  
  override def handleI2b2(handler: I2b2RequestHandler, shouldBroadcast: Boolean) = handler.readQueryDefinition(this, shouldBroadcast)

  override def toXml = XmlUtil.stripWhitespace {
    <readQueryDefinition>
      { headerFragment }
      <queryId>{ queryId }</queryId>
    </readQueryDefinition>
  }
  
  protected override def i2b2MessageBody = XmlUtil.stripWhitespace {
    <message_body>
      { i2b2PsmHeader }
      <ns4:request xsi:type="ns4:master_requestType" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
        <query_master_id>{ queryId }</query_master_id>
      </ns4:request>
    </message_body>
  }
}

object ReadQueryDefinitionRequest extends AbstractReadQueryDefinitionRequest.Companion(new ReadQueryDefinitionRequest(_, _, _, _))
