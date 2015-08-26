package net.shrine.protocol

import scala.collection.mutable.WrappedArray
import net.shrine.serialization.XmlMarshaller
import scala.xml.NodeSeq
import net.shrine.util.{Base64, XmlUtil}
import net.shrine.serialization.XmlUnmarshaller
import scala.util.Try
import java.security.cert.CertificateFactory
import java.io.ByteArrayInputStream
import java.security.cert.X509Certificate

/**
 * @author clint
 * @since Dec 4, 2014
 */
final case class CertData(value: WrappedArray[Byte]) extends XmlMarshaller {
  
  override def toXml: NodeSeq = XmlUtil.stripWhitespace {
    <certData>
      { Base64.toBase64(value.array) }
    </certData>
  }
  
  override def toString: String = {
    //NB: Don't use WrappedArray's super-verbose .toString
    s"CertData(${Base64.toBase64(value.array)})"
  }
  
  def toCertificate: X509Certificate = {
    val inputStream = new ByteArrayInputStream(value.toArray)
    
    try { CertData.certFactory.generateCertificate(inputStream).asInstanceOf[X509Certificate] }
    finally { inputStream.close() }
  }
}

object CertData extends XmlUnmarshaller[Try[CertData]] {
  val certificateFormat = "X.509"
  
  private lazy val certFactory: CertificateFactory = CertificateFactory.getInstance(certificateFormat)
  
  def apply(cert: X509Certificate): CertData = CertData(cert.getEncoded)
  
  override def fromXml(xml: NodeSeq): Try[CertData] = {
    import XmlUtil.trim

    for {
      value <- Try(xml.head).map(trim).filter(!_.isEmpty).map(Base64.fromBase64)
    } yield CertData(value)
  }
}