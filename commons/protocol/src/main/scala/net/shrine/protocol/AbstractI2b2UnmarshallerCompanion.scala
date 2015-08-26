package net.shrine.protocol

import net.shrine.serialization.I2b2Unmarshaller
import scala.xml.NodeSeq
import net.shrine.serialization.XmlUnmarshaller
import scala.util.Try

/**
 * @author clint
 * @date Mar 29, 2013
 * 
 * Abstract base for companion objects with methods for unmarshalling CRC requests
 */
abstract class AbstractI2b2UnmarshallerCompanion[Req <: ShrineRequest](i2b2CrcRequestUnmarshallers: Map[CrcRequestType, I2b2XmlUnmarshaller[Req]]) extends I2b2XmlUnmarshaller[Req] {

  private val crcRequestUnmarshallersByI2b2RequestType: Map[String, I2b2XmlUnmarshaller[Req]] = {
    i2b2CrcRequestUnmarshallers.map { case (rt, u) => (rt.i2b2RequestType, u) }
  }

  protected def isPsmRequest(xml: NodeSeq): Boolean = {
    val hasPsmHeader = hasMessageBodySubElement(xml, "psmheader") 
    
    val hasRequestType = requestType(xml).nonEmpty
    
    hasPsmHeader && hasRequestType
  }

  protected def hasMessageBodySubElement(xml: NodeSeq, tagName: String): Boolean = {
    (xml \ "message_body" \ tagName).nonEmpty
  }
  
  private def requestType(xml: NodeSeq): NodeSeq = xml \ "message_body" \ "psmheader" \ "request_type"
  
  protected def parsePsmRequest(breakdownTypes: Set[ResultOutputType], xml: NodeSeq): Try[Req] = {
    val incomingRequestType = requestType(xml).text

    crcRequestUnmarshallersByI2b2RequestType.get(incomingRequestType) match {
      case None => scala.util.Failure(new Exception(s"Unknown request type: '$incomingRequestType'"))
      case Some(u) => u.fromI2b2(breakdownTypes)(xml)
    }
  }
}