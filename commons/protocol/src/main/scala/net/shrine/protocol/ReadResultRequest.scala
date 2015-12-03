package net.shrine.protocol

import scala.xml.NodeSeq
import net.shrine.util.XmlUtil
import net.shrine.serialization.XmlMarshaller
import net.shrine.serialization.I2b2Marshaller
import net.shrine.serialization.I2b2Unmarshaller
import scala.concurrent.duration.Duration
import scala.util.Try
import net.shrine.util.NodeSeqEnrichments
import net.shrine.serialization.I2b2UnmarshallingHelpers

/**
 * @author clint
 * @date Aug 16, 2012
 */
final case class ReadResultRequest(
  override val projectId: String,
  override val waitTime: Duration,
  override val authn: AuthenticationInfo,
  val localResultId: String) extends ShrineRequest(projectId, waitTime, authn) with CrcRequest with TranslatableRequest[ReadResultRequest] {

  //NB: Needs to be a TranslatableRequest so that AbstractReadQueryResultAdapter doesn't have to manually 
  //add the correct projectId and credentials to requests (like this one) that it sends to the CRC. 

  def this(header: RequestHeader, localResultId: String) = this(header.projectId, header.waitTime, header.authn, localResultId)

  override val requestType = RequestType.ResultRequest

  //NB: This request is never sent through the broadcaster-aggregator/shrine service, so it doesn't make sense
  //to have it be handled by a ShrineRequestHandler.

  override protected def i2b2MessageBody: NodeSeq = XmlUtil.stripWhitespace {
    <message_body>
      { i2b2PsmHeader }
      <ns4:request xsi:type="ns4:result_requestType" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
        <query_result_instance_id>{ localResultId }</query_result_instance_id>
      </ns4:request>
    </message_body>
  }

  override def toXml: NodeSeq = XmlUtil.stripWhitespace {
    <readResult>
      { headerFragment }
      <resultId>{ localResultId }</resultId>
    </readResult>
  }

  override def withAuthn(newAuthn: AuthenticationInfo) = this.copy(authn = newAuthn)

  override def withProject(newProjectId: String) = this.copy(projectId = newProjectId)

  override def asRequest: ReadResultRequest = this
}

object ReadResultRequest extends I2b2XmlUnmarshaller[ReadResultRequest] with ShrineXmlUnmarshaller[ReadResultRequest] with ShrineRequestUnmarshaller with I2b2UnmarshallingHelpers {
  override def fromXml(breakdownTypes: Set[ResultOutputType])(xml: NodeSeq): Try[ReadResultRequest] = {
    import NodeSeqEnrichments.Strictness._

    for {
      header <- shrineHeader(xml)
      //NB: This is the LOCAL, NOT NETWORK, resultId
      resultId <- xml.withChild("resultId").map(_.text)
    } yield {
      new ReadResultRequest(header, resultId)
    }
  }

  override def fromI2b2(breakdownTypes: Set[ResultOutputType])(xml: NodeSeq): Try[ReadResultRequest] = {
    import NodeSeqEnrichments.Strictness._

    for {
      header <- i2b2Header(xml)
      //NB: This is the LOCAL, NOT NETWORK, resultId
      resultId <- (xml withChild "message_body" withChild "request" withChild "query_result_instance_id").map(_.text)
    } yield {
      new ReadResultRequest(header, resultId)
    }
  }
}