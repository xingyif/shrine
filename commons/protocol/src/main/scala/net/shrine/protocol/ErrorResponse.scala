package net.shrine.protocol

import net.shrine.log.Loggable
import net.shrine.problem.{ProblemNotYetEncoded, LoggingProblemHandler, Problem, ProblemDigest}

import scala.xml.{NodeBuffer, NodeSeq}
import net.shrine.util.XmlUtil
import net.shrine.serialization.XmlUnmarshaller
import net.shrine.serialization.I2b2Unmarshaller
import net.shrine.util.NodeSeqEnrichments
import scala.util.Try
import scala.util.control.NonFatal

/**
 * @author Bill Simons
 * @since 4/25/11
 * @see http://cbmi.med.harvard.edu
 * @see http://chip.org
 *       <p/>
 *       NOTICE: This software comes with NO guarantees whatsoever and is
 *       licensed as Lgpl Open Source
 * @see http://www.gnu.org/licenses/lgpl.html
 *
 * NB: Now a case class for structural equality
 */
final case class ErrorResponse(errorMessage: String,problemDigest:ProblemDigest) extends ShrineResponse {

  override protected def status: NodeSeq = {
    val buffer = new NodeBuffer
    buffer += <status type="ERROR">{ errorMessage }</status>
    buffer += problemDigest.toXml
  }

  override protected def i2b2MessageBody = null

  import ErrorResponse.rootTagName

  override def toXml = { XmlUtil.stripWhitespace {
      val xml = XmlUtil.renameRootTag(rootTagName) {
        <errorResponse>
          <message>{ errorMessage }</message>
          {problemDigest.toXml}
        </errorResponse>
      }
      xml
    }
  }
}

object ErrorResponse extends XmlUnmarshaller[ErrorResponse] with I2b2Unmarshaller[ErrorResponse] with HasRootTagName with Loggable {
  val rootTagName = "errorResponse"

  //todo delete this one
  def apply(errorMessage:String,problem:Option[Problem] = None):ErrorResponse = {
    val p = problem.getOrElse(ProblemNotYetEncoded(s"'$errorMessage'"))
    LoggingProblemHandler.handleProblem(p) //todo someday hook up to the proper problem handler hierarchy.
    ErrorResponse(errorMessage,p.toDigest)
  }

  def apply(problem:Problem):ErrorResponse = {
    LoggingProblemHandler.handleProblem(problem) //todo someday hook up to the proper problem handler hierarchy.
    new ErrorResponse(problem.summary,problem.toDigest)
  }


  override def fromXml(xml: NodeSeq): ErrorResponse = {

    val messageXml = xml \ "message"

    //NB: Fail fast
    require(messageXml.nonEmpty)

    val problemDigest = ProblemDigest.fromXml(xml)

    ErrorResponse(XmlUtil.trim(messageXml),problemDigest)
  }

  override def fromI2b2(xml: NodeSeq): ErrorResponse = {
    import NodeSeqEnrichments.Strictness._

    //todo what determines parseFormatA vs parseFormatB when written? It looks like our ErrorResponses use A.

    def parseFormatA: Try[ErrorResponse] = {
      for {
        statusXml <- xml withChild "response_header" withChild "result_status" withChild "status"
        resultStatusXml <- xml withChild "response_header" withChild "result_status"
        typeText <- statusXml attribute "type"    if typeText == "ERROR" //NB: Fail fast{
                                                    statusMessage = XmlUtil.trim(statusXml)
                                                    problemDigest = ProblemDigest.fromXml(resultStatusXml)
      } yield {
        ErrorResponse(statusMessage,problemDigest)
      }
    }

    def parseFormatB: Try[ErrorResponse] = {
      for {
        conditionXml <- xml withChild "message_body" withChild "response" withChild "status" withChild "condition"
        typeText <- conditionXml attribute "type" if typeText == "ERROR"
                                                    statusMessage = XmlUtil.trim(conditionXml)
                                                    problemDigest = ErrorStatusFromCrc(Option(statusMessage),xml.text).toDigest//here's another place where an ERROR can have no ProblemDigest
      } yield {
        ErrorResponse(statusMessage,problemDigest)
      }
    }

    parseFormatA.recoverWith { case NonFatal(e) => {
      warn(s"Encountered a problem while parsing an error from I2B2 with 'format A', trying 'format B' ${xml.text}",e)
      parseFormatB
    } }.get
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