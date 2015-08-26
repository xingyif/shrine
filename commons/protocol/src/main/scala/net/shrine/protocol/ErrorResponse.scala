package net.shrine.protocol

import xml.NodeSeq
import net.shrine.util.XmlUtil
import net.shrine.serialization.XmlUnmarshaller
import net.shrine.serialization.I2b2Unmarshaller
import net.shrine.util.NodeSeqEnrichments
import scala.util.Try
import scala.util.control.NonFatal

/**
 * @author Bill Simons
 * @date 4/25/11
 * @link http://cbmi.med.harvard.edu
 * @link http://chip.org
 *       <p/>
 *       NOTICE: This software comes with NO guarantees whatsoever and is
 *       licensed as Lgpl Open Source
 * @link http://www.gnu.org/licenses/lgpl.html
 *
 * NB: Now a case class for structural equality
 */
final case class ErrorResponse(val errorMessage: String) extends ShrineResponse {

  override protected def status = <status type="ERROR">{ errorMessage }</status>

  override protected def i2b2MessageBody = null

  import ErrorResponse.rootTagName

  override def toXml = XmlUtil.stripWhitespace {
    XmlUtil.renameRootTag(rootTagName) {
      <errorResponse>
        <message>{ errorMessage }</message>
      </errorResponse>
    }
  }
}

object ErrorResponse extends XmlUnmarshaller[ErrorResponse] with I2b2Unmarshaller[ErrorResponse] with HasRootTagName {
  val rootTagName = "errorResponse"

  override def fromXml(xml: NodeSeq): ErrorResponse = {
    val messageXml = (xml \ "message")

    //NB: Fail fast
    require(messageXml.nonEmpty)

    ErrorResponse(XmlUtil.trim(messageXml))
  }

  override def fromI2b2(xml: NodeSeq): ErrorResponse = {
    import NodeSeqEnrichments.Strictness._

    def parseFormatA: Try[ErrorResponse] = {
      for {
        statusXml <- xml withChild "response_header" withChild "result_status" withChild "status"
        typeText <- (statusXml attribute "type")
        if typeText == "ERROR" //NB: Fail fast
        statusMessage = XmlUtil.trim(statusXml)
      } yield {
        ErrorResponse(statusMessage)
      }
    }

    def parseFormatB: Try[ErrorResponse] = {
      for {
        conditionXml <- xml withChild "message_body" withChild "response" withChild "status" withChild "condition"
        typeText <- conditionXml attribute "type"
        if typeText == "ERROR"
        statusMessage = XmlUtil.trim(conditionXml)
      } yield {
        ErrorResponse(statusMessage)
      }
    }

    parseFormatA.recoverWith { case NonFatal(e) => parseFormatB }.get
  }

  /**
   *
   * <ns5:response>
   * <message_body>
   * <ns4:response xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="ns4:crc_xml_result_responseType">
   * <status>
   * <condition type="ERROR">Query result instance id 3126 not found</condition>
   * </status>
   * </ns4:response>
   * </message_body>
   * </ns5:response>
   */
}