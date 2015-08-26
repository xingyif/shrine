package net.shrine.serialization

import scala.xml.NodeSeq
import scala.util.Try
import net.shrine.protocol.RequestHeader
import net.shrine.util.{Tries, NodeSeqEnrichments}
import scala.concurrent.duration.Duration
import net.shrine.protocol.RequestType
import net.shrine.protocol.CrcRequestType
import net.shrine.protocol.AuthenticationInfo

/**
 * @author clint
 * @since Oct 30, 2014
 */
trait I2b2UnmarshallingHelpers {
  final def i2b2Header(xml: NodeSeq): Try[RequestHeader] = {
    for {
      projectId <- i2b2ProjectId(xml)
      waitTime <- i2b2WaitTime(xml)
      authn <- i2b2AuthenticationInfo(xml)
    } yield {
      RequestHeader(projectId, waitTime, authn)
    }
  }
  
  import NodeSeqEnrichments.Strictness._
  
  final def i2b2ProjectId(xml: NodeSeq): Try[String] = (xml withChild "message_header" withChild "project_id").map(_.text)

  final def i2b2WaitTime(xml: NodeSeq): Try[Duration] = {
    import scala.concurrent.duration._
    
    (xml withChild "request_header" withChild "result_waittime_ms").map(_.text.toLong.milliseconds)
  }

  final def i2b2AuthenticationInfo(xml: NodeSeq): Try[AuthenticationInfo] = (xml withChild "message_header" withChild "security").flatMap(AuthenticationInfo.fromI2b2)
  
  final def i2b2RequestType(xml: NodeSeq): Try[RequestType] = {
    import Tries.toTry
    
    lazy val error = new Exception(s"Couldn't determine CRC request type from '$xml'")
    
    for {
      i2b2ReqTypeName <- xml.withChild("message_body").withChild("psmheader").withChild("request_type").map(_.text.trim)
      crcRequestType <-toTry(CrcRequestType.withI2b2RequestType(i2b2ReqTypeName))(error)
      requestType <- toTry(RequestType.withCrcRequestType(crcRequestType))(error)
    } yield requestType
  }
}