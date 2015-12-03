package net.shrine.protocol

import xml.NodeSeq
import net.shrine.serialization.{ I2b2Marshaller, I2b2Unmarshaller, XmlMarshaller, XmlUnmarshaller }
import scala.util.Try
import net.shrine.util.NodeSeqEnrichments
import scala.util.Success
import scala.util.control.NonFatal

/**
 * @author Bill Simons
 * @date 3/9/11
 * @link http://cbmi.med.harvard.edu
 * @link http://chip.org
 *       <p/>
 *       NOTICE: This software comes with NO guarantees whatsoever and is
 *       licensed as Lgpl Open Source
 * @link http://www.gnu.org/licenses/lgpl.html
 *
 * NB: this is a case class to get a structural equality contract in hashCode and equals, mostly for testing
 */
final case class Credential(val value: String, val isToken: Boolean) extends XmlMarshaller with I2b2Marshaller {

  override def toXml = <credential isToken={ isToken.toString }>{ value }</credential>

  override def toI2b2 = <password token_ms_timeout="1800000" is_token={ isToken.toString }>{ value }</password>
}

object Credential extends I2b2Unmarshaller[Try[Credential]] with XmlUnmarshaller[Try[Credential]] {

  override def fromI2b2(xml: NodeSeq): Try[Credential] = parse(xml)(parseI2b2IsToken)

  override def fromXml(xml: NodeSeq): Try[Credential] = parse(xml)(parseShrineIsToken)

  private def parse(xml: NodeSeq)(parseIsToken: NodeSeq => Boolean): Try[Credential] = Try {
    Credential(xml.text, parseIsToken(xml))
  }

  private def parseI2b2IsToken(xml: NodeSeq): Boolean = parseIsToken(xml, "is_token")

  private def parseShrineIsToken(xml: NodeSeq): Boolean = parseIsToken(xml, "isToken")

  private def parseIsToken(xml: NodeSeq, attribute: String): Boolean = {
    val isTokenXml = xml \ ("@" + attribute)

    if (isTokenXml.isEmpty) { false }
    else { isTokenXml.text.toBoolean }
  }
}