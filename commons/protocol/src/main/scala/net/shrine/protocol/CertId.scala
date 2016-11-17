package net.shrine.protocol

import java.math.BigInteger
import scala.util.Try
import scala.xml.NodeSeq
import net.shrine.serialization.XmlMarshaller
import net.shrine.serialization.XmlUnmarshaller
import net.shrine.util.XmlUtil
import net.shrine.util.OptionEnrichments

/**
 * @author clint
 * @date Nov 22, 2013
 */
final case class CertId(serial: BigInteger, name: Option[String] = None) extends XmlMarshaller {
  override def toXml: NodeSeq = XmlUtil.stripWhitespace {
    import OptionEnrichments._
    Nil
    
    <certId>
      <serial>{ serial }</serial>
      { name.toXml(<name/>) }
    </certId>
  }

  override def equals(other: Any): Boolean = other match {
    case that: CertId => that.serial == this.serial
    case _ => false
  }

  override def hashCode: Int = serial.hashCode
}

object CertId extends XmlUnmarshaller[Try[CertId]] {
  override def fromXml(xml: NodeSeq): Try[CertId] = {
    for {
      serial <- Try(new BigInteger((xml \ "serial").text.trim))
      nameOption = (xml \ "name").headOption.map(_.text.trim)
    } yield CertId(serial, nameOption)
  }
}