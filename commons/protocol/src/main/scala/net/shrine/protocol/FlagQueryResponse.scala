package net.shrine.protocol

import scala.xml.NodeSeq
import net.shrine.serialization.XmlUnmarshaller
import scala.util.Try
import net.shrine.util.XmlUtil
import net.shrine.serialization.I2b2Unmarshaller
import net.shrine.util.NodeSeqEnrichments

/**
 * @author clint
 * @date Mar 26, 2014
 * 
 * NB: Now needs to be serializable as an i2b2 blob, because even though flagging is a Shrine-only feature,
 * flagging will be initiated from the legacy i2b2 webclient for the forseeable future.  In that context,
 * generating i2b2 XML blobs and running them through the proxy is the path of least resistance. Sigh. 
 * - 17 June 2014
 */
trait FlagQueryResponse extends ShrineResponse {
  override protected def i2b2MessageBody: NodeSeq = rootTag
  
  override def toXml: NodeSeq = rootTag
  
  private lazy val rootTag = XmlUtil.renameRootTag(FlagQueryResponse.rootTagName)(<placeHolder/>)
}

object FlagQueryResponse extends FlagQueryResponse with XmlUnmarshaller[Try[FlagQueryResponse]] with I2b2Unmarshaller[Try[FlagQueryResponse]] with HasRootTagName { self =>
  override val rootTagName = "flagQueryResponse"
  
  override def fromXml(xml: NodeSeq): Try[FlagQueryResponse] = {
    Try(xml.head.label).filter(_ == rootTagName).map(_ => self)
  }
  
  override def fromI2b2(xml: NodeSeq): Try[FlagQueryResponse] = {
    import NodeSeqEnrichments.Strictness._
    
    xml.withChild("message_body").withChild(rootTagName).map(_ => self)
  }
}