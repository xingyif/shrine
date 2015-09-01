package net.shrine.protocol

import net.shrine.serialization.I2b2Unmarshaller
import scala.xml.NodeSeq
import CrcRequestType._
import scala.util.Try
import net.shrine.util.NodeSeqEnrichments

/**
 * @author clint
 * @since Feb 19, 2014
 */
abstract class AbstractShrineRequestI2b2UnmarshallerCompanion[Req <: ShrineRequest](i2b2CrcRequestUnmarshallers: Map[CrcRequestType, I2b2XmlUnmarshaller[Req]]) extends AbstractI2b2UnmarshallerCompanion[Req](i2b2CrcRequestUnmarshallers) {

  override def fromI2b2(breakdownTypes: Set[ResultOutputType])(i2b2Request: NodeSeq): Try[Req] = {
    if (i2b2Request == null) { scala.util.Failure(new Exception(s"Null request")) }
    else {
      matchers.find(_.isDefinedAt(i2b2Request)) match {
        case Some(unmarshal) => unmarshal(i2b2Request)(breakdownTypes)
        case None => scala.util.Failure(new Exception(s"Request not understood: $i2b2Request"))
      }
    }
  }

  import ReadResultOutputTypesRequest.isReadResultOutputTypesRequest
  
  private val matchers: Seq[PartialFunction[NodeSeq, Set[ResultOutputType] => Try[Req]]] = Seq(
    //NB: Cast is needed, "safe", but not nice
    { case i2b2Request if isReadResultOutputTypesRequest(i2b2Request) => breakdownTypes => ReadResultOutputTypesRequest.fromI2b2(breakdownTypes)(i2b2Request).asInstanceOf[Try[Req]] },
    { case i2b2Request if isPsmRequest(i2b2Request) => breakdownTypes => parsePsmRequest(breakdownTypes, i2b2Request) },
    //NB: Cast is needed, "safe", but not nice
    { case i2b2Request if isPdoRequest(i2b2Request) => breakdownTypes => parsePdoRequest(breakdownTypes, i2b2Request).asInstanceOf[Try[Req]] },
    //NB: Cast is needed, "safe", but not nice
    { case i2b2Request if isSheriffRequest(i2b2Request) => breakdownTypes => parseSheriffRequest(breakdownTypes, i2b2Request).asInstanceOf[Try[Req]] },
    //NB: Cast is needed, "safe", but not nice
    { case i2b2Request if isFlagRequest(i2b2Request) => breakdownTypes => FlagQueryRequest.fromI2b2(breakdownTypes)(i2b2Request).asInstanceOf[Try[Req]] },
    //NB: Cast is needed, "safe", but not nice
    { case i2b2Request if isUnFlagRequest(i2b2Request) => breakdownTypes => UnFlagQueryRequest.fromI2b2(breakdownTypes)(i2b2Request).asInstanceOf[Try[Req]] })

  protected def isPdoRequest(requestXml: NodeSeq): Boolean = hasMessageBodySubElement(requestXml, "pdoheader")

  protected def isSheriffRequest(requestXml: NodeSeq): Boolean = hasMessageBodySubElement(requestXml, "sheriff_header")

  protected def isI2b2AdminPreviousQueriesRequest(requestXml: NodeSeq): Boolean = hasMessageBodySubElement(requestXml, "get_name_info")

  protected def isFlagRequest(requestXml: NodeSeq): Boolean = hasRequestSubElement(requestXml, FlagQueryRequest.rootTagName)

  protected def isUnFlagRequest(requestXml: NodeSeq): Boolean = hasRequestSubElement(requestXml, UnFlagQueryRequest.rootTagName)

  private def hasRequestSubElement(requestXml: NodeSeq, tagName: String): Boolean = (requestXml \ "message_body" \ "request" \ tagName).nonEmpty

  private def parsePdoRequest(breakdownTypes: Set[ResultOutputType], requestXml: NodeSeq): Try[ReadPdoRequest] = {
    import NodeSeqEnrichments.Strictness._

    for {
      requestTypeText <- (requestXml withChild "message_body" withChild "pdoheader" withChild "request_type").map(_.text)
      if requestTypeText == GetPDOFromInputListRequestType.i2b2RequestType
      req <- ReadPdoRequest.fromI2b2(breakdownTypes)(requestXml)
    } yield req
  }

  private def parseSheriffRequest(breakdownTypes: Set[ResultOutputType], xml: NodeSeq) = ReadApprovedQueryTopicsRequest.fromI2b2(breakdownTypes)(xml)

  private def parseI2b2AdminPreviousQueriesRequest(breakdownTypes: Set[ResultOutputType], xml: NodeSeq) = ReadI2b2AdminPreviousQueriesRequest.fromI2b2(breakdownTypes)(xml)
}