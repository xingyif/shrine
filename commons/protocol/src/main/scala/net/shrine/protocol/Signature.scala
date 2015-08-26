package net.shrine.protocol

import scala.collection.mutable.WrappedArray
import scala.util.Try
import scala.xml.NodeSeq
import javax.xml.datatype.XMLGregorianCalendar
import net.shrine.serialization.XmlMarshaller
import net.shrine.serialization.XmlUnmarshaller
import net.shrine.util.{Base64, XmlDateHelper, XmlUtil, NodeSeqEnrichments}

/**
 * @author clint
 * @since Nov 22, 2013
 */
final case class Signature(
    timestamp: XMLGregorianCalendar, 
    signedBy: CertId,
    signingCert: Option[CertData],
    //NB: Use a WrappedArray here to get the usual case class by-value comparison semantics
    //The case class boilerplate invokes _.hashCode and _.equals, which for regular arrays
    //compare by reference, not by value.
    value: WrappedArray[Byte]) extends XmlMarshaller {
  
  import XmlUtil._
  
  override def toXml: NodeSeq = stripWhitespace {
    <signature>
      <timestamp>{ timestamp }</timestamp>
      { XmlUtil.renameRootTag("signedBy")(signedBy.toXml.head) }
      { signingCert.map(_.toXml.head).map(renameRootTag("signingCert")).orNull }
      <value>{ Base64.toBase64(value.array) }</value>
    </signature>
  }
  
  override def toString: String = {
    //NB: Don't use WrappedArray's super-verbose .toString
    s"Signature($timestamp,$signedBy,$signingCert,${Base64.toBase64(value.array)})"
  }
}

object Signature extends XmlUnmarshaller[Try[Signature]] {
  import NodeSeqEnrichments.Strictness._
  import XmlUtil.trim
  
  override def fromXml(xml: NodeSeq): Try[Signature] = {
    for {
      timestamp <- xml.withChild("timestamp").map(trim).flatMap(XmlDateHelper.parseXmlTime)
      signedBy <- xml.withChild("signedBy").flatMap(CertId.fromXml)
      signingCert = xml.withChild("signingCert").flatMap(CertData.fromXml).toOption
      value <- xml.withChild("value").map(trim).map(Base64.fromBase64)
    } yield Signature(timestamp, signedBy, signingCert, value)
  }
}