package net.shrine.protocol

import scala.concurrent.duration.Duration
import scala.xml.NodeSeq
import net.shrine.util.XmlUtil
import net.shrine.serialization.I2b2Unmarshaller
import scala.util.Try
import net.shrine.util.NodeSeqEnrichments
import scala.util.Success
import net.shrine.serialization.I2b2UnmarshallingHelpers

/**
 * @author clint
 * @date Oct 2, 2014
 */
final case class ReadResultOutputTypesRequest(
  override val projectId: String,
  override val waitTime: Duration,
  override val authn: AuthenticationInfo) extends ShrineRequest(projectId, waitTime, authn) with HandleableI2b2Request with NonI2b2ableRequest with HasHeaderFields {

  override val requestType = ReadResultOutputTypesRequest.requestType

  override def handleI2b2(handler: I2b2RequestHandler, shouldBroadcast: Boolean): ShrineResponse = {
    handler.readResultOutputTypes(this)
  }

  override def toXml: NodeSeq = XmlUtil.stripWhitespace {
    <readResultOutputTypesRequest>
      { headerFragment }
    </readResultOutputTypesRequest>
  }
}

object ReadResultOutputTypesRequest extends I2b2XmlUnmarshaller[ReadResultOutputTypesRequest] with I2b2UnmarshallingHelpers {

  val requestType = RequestType.GetResultOutputTypesRequest
  
  def isReadResultOutputTypesRequest(xml: NodeSeq): Boolean = {
    i2b2RequestType(xml).map(_ == requestType).getOrElse(false)
  }

  override def fromI2b2(breakdownTypes: Set[ResultOutputType])(xml: NodeSeq): Try[ReadResultOutputTypesRequest] = {
    import scala.util.Failure

    if (isReadResultOutputTypesRequest(xml)) {
      for {
        projectId <- i2b2ProjectId(xml)
        waitTime <- i2b2WaitTime(xml)
        authn <- i2b2AuthenticationInfo(xml)
      } yield {
        ReadResultOutputTypesRequest(projectId, waitTime, authn)
      }
    } else {
      val expectedI2b2ReqType = requestType.crcRequestType.map(_.i2b2RequestType).getOrElse("")

      Failure(new Exception(s"Request type '$requestType' ($expectedI2b2ReqType) not found in '$xml'"))
    }
  }
}