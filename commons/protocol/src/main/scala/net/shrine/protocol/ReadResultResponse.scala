package net.shrine.protocol

import scala.xml.NodeSeq
import net.shrine.serialization.I2b2Unmarshaller
import net.shrine.serialization.XmlUnmarshaller
import net.shrine.util.XmlUtil
import scala.util.Try
import scala.util.Success
import net.shrine.util.NodeSeqEnrichments

/**
 * @author clint
 * @since Aug 17, 2012
 */
final case class ReadResultResponse(xmlResultId: Long, metadata: QueryResult, data: I2b2ResultEnvelope) extends ShrineResponse {
  override protected def i2b2MessageBody: NodeSeq = XmlUtil.stripWhitespace {
    <ns4:response xsi:type="ns4:crc_xml_result_responseType">
      <status>
        <condition type="DONE">DONE</condition>
      </status>
      { metadata.toI2b2 }
      <crc_xml_result>
        <xml_result_id>{ xmlResultId }</xml_result_id>
        <result_instance_id>{ metadata.resultId }</result_instance_id>
        <xml_value>
          {
            I2b2Workarounds.escape("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""") +
              I2b2Workarounds.escape(data.toI2b2String)
          }
        </xml_value>
      </crc_xml_result>
    </ns4:response>
  }

  //xmlResultId doesn't seem necessary, but I wanted to allow Shrine => I2b2 => Shrine marshalling loops without losing anything.  
  //Maybe this isn't needed? 
  override def toXml: NodeSeq = XmlUtil.stripWhitespace {
    <readResultResponse>
      { metadata.toXml }
      <xmlResultId>{ xmlResultId }</xmlResultId>
      { data.toXml }
    </readResultResponse>
  }
}

object ReadResultResponse extends HasRootTagName {

  override val rootTagName = "readResultResponse"

  import NodeSeqEnrichments.Strictness._
  
  def fromXml(breakdownTypes: Set[ResultOutputType])(xml: NodeSeq): Try[ReadResultResponse] = unmarshal(
    xml,
    _.withChild("resultEnvelope").flatMap(I2b2ResultEnvelope.fromXml(breakdownTypes)),
    _.withChild("queryResult").map(QueryResult.fromXml(breakdownTypes)),
    _.withChild("xmlResultId").map(_.text.toLong))

  private[this] def messageBodyXml(x: NodeSeq): Try[NodeSeq] = x.withChild("message_body")

  private[this] def responseXml(x: NodeSeq) = messageBodyXml(x).withChild("response")

  private[this] def crcResultXml(x: NodeSeq): Try[NodeSeq] = {
    val crcXmlResult: Try[NodeSeq] = responseXml(x).withChild("crc_xml_result")
//    crcXmlResult.transform({xml:NodeSeq => Success(xml)},{e:Throwable => FailureResult(x)})
    crcXmlResult
  }

  def fromI2b2(breakdownTypes: Set[ResultOutputType])(xml: NodeSeq): Try[ReadResultResponse] = {
    def getEnvelope(x: NodeSeq): Try[I2b2ResultEnvelope] = {
      for {
        escapedXml <- crcResultXml(x).withChild("xml_value").map(_.text)
        unescapedXml = I2b2Workarounds.unescape(escapedXml)
        envelope <- I2b2ResultEnvelope.fromI2b2String(breakdownTypes)(unescapedXml)
      } yield envelope
    }

    def getXmlResultId(x: NodeSeq): Try[Long] = crcResultXml(x).withChild("xml_result_id").map(_.text.toLong)

    def getQueryResult(x: NodeSeq): Try[QueryResult] = {
      responseXml(x).withChild("query_result_instance").map(QueryResult.fromI2b2(breakdownTypes))
    }
    
    unmarshal(
      xml,
      getEnvelope,
      getQueryResult,
      getXmlResultId)
  }

  private def unmarshal(
    nodeSeq: NodeSeq,
    getData: NodeSeq => Try[I2b2ResultEnvelope],
    getMetadata: NodeSeq => Try[QueryResult],
    getXmlResultId: NodeSeq => Try[Long]): Try[ReadResultResponse] = {

    for {
      xml <- Success(nodeSeq)
      data <- getData(xml)
      metadata <- getMetadata(xml)
      xmlResultId <- getXmlResultId(xml)
    } yield ReadResultResponse(xmlResultId, metadata, data)
  }
}
