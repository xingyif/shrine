package net.shrine.protocol

import scala.xml.NodeSeq
import net.shrine.util.XmlUtil
import scala.util.Try
import net.shrine.serialization.XmlUnmarshaller
import net.shrine.util.NodeSeqEnrichments
import net.shrine.serialization.I2b2Unmarshaller

/**
 * @author clint
 * @date Apr 18, 2014
 * 
 * NB: Now needs to be serializable as an i2b2 blob, because even though flagging is a Shrine-only feature,
 * flagging will be initiated from the legacy i2b2 webclient for the forseeable future.  In that context,
 * generating i2b2 XML blobs and running them through the proxy is the path of least resistance. Sigh. 
 * - 17 June 2014
 */
trait UnFlagQueryResponse extends ShrineResponse {
  override protected def i2b2MessageBody: NodeSeq = rootTag
  
  override def toXml: NodeSeq = rootTag
  
  private lazy val rootTag = XmlUtil.renameRootTag(UnFlagQueryResponse.rootTagName)(<placeHolder/>)
}

/**
 * NB: There is no fromI2b2() method, because this will never be sent by an i2b2 component (CRC, legacy web client)
 * and so Shrine will never need to parse this response from i2b2 format
 */
object UnFlagQueryResponse extends UnFlagQueryResponse with XmlUnmarshaller[Try[UnFlagQueryResponse]] with I2b2Unmarshaller[Try[UnFlagQueryResponse]] with HasRootTagName { self =>
  override val rootTagName = "unFlagQueryResponse"
  
  override def fromXml(xml: NodeSeq): Try[UnFlagQueryResponse] = {
    Try(xml.head.label).filter(_ == rootTagName).map(_ => self)
  }
  
  override def fromI2b2(xml: NodeSeq): Try[UnFlagQueryResponse] = {
    import NodeSeqEnrichments.Strictness._
    
    xml.withChild("message_body").withChild(rootTagName).map(_ => self)
  }
}