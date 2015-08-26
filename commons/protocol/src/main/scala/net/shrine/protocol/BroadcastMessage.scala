package net.shrine.protocol

import scala.xml.NodeSeq
import net.shrine.serialization.XmlMarshaller
import net.shrine.serialization.XmlUnmarshaller
import net.shrine.util.XmlUtil
import scala.util.Try
import net.shrine.util.NodeSeqEnrichments

/**
 * @author Bill Simons
 * @date 4/5/11
 * @link http://cbmi.med.harvard.edu
 */
final case class BroadcastMessage(
    requestId: Long, 
    networkAuthn: AuthenticationInfo, 
    request: BaseShrineRequest, 
    signature: Option[Signature] = None) extends ShrineMessage {

  def withRequestId(id: Long): BroadcastMessage = this.copy(requestId = id)

  def withRequest(req: ShrineRequest): BroadcastMessage = this.copy(request = req)

  def withSignature(sig: Signature): BroadcastMessage = this.copy(signature = Option(sig))
  
  override def toXml: NodeSeq = XmlUtil.stripWhitespace {
    <broadcastMessage>
      <requestId>{ requestId }</requestId>
      { networkAuthn.toXml }
      { signature.map(_.toXml).orNull }
      <request>{ request.toXml }</request>
    </broadcastMessage>
  }
}

object BroadcastMessage extends XmlUnmarshaller[Try[BroadcastMessage]] {
  def apply(networkAuthn: AuthenticationInfo, request: BaseShrineRequest): BroadcastMessage = BroadcastMessage(Ids.next, networkAuthn, request)

  /**
   * @author clint
   * @date Nov 29, 2012
   */
  object Ids {
    private val random = new java.util.Random

    def next: Long = random.nextLong.abs
  }

  override def fromXml(xml: NodeSeq): Try[BroadcastMessage] = {
    import NodeSeqEnrichments.Strictness._
    
    for {
      id <- (xml withChild "requestId").map(_.text.toLong)
      authn <- (xml withChild AuthenticationInfo.shrineXmlTagName).flatMap(AuthenticationInfo.fromXml)
      reqXml <- xml withChild "request"
      req <- BaseShrineRequest.fromXml(Set.empty)(reqXml \ "_")
      sigOption = Signature.fromXml(xml \ "signature").toOption
    } yield BroadcastMessage(id, authn, req, sigOption)
  }
}