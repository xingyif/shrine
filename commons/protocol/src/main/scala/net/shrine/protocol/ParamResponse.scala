package net.shrine.protocol

import xml.NodeSeq
import net.shrine.util.XmlUtil
import net.shrine.serialization.{I2b2Unmarshaller, XmlUnmarshaller}

/**
 * @author ??
 * @date ??
 * @link http://cbmi.med.harvard.edu
 * @link http://chip.org
 *       <p/>
 *       NOTICE: This software comes with NO guarantees whatsoever and is
 *       licensed as Lgpl Open Source
 * @link http://www.gnu.org/licenses/lgpl.html
 * 
 * NB: a case class for structural equals() and hashCode()
 */
final case class ParamResponse(val name: String, val column: String, val value: String) extends ShrineResponse {

  override def i2b2MessageBody = toXml

  override def toXml = XmlUtil.stripWhitespace(
    <param name={name} column={column}>
      {value}
    </param>)
}

object ParamResponse extends I2b2Unmarshaller[ParamResponse] with XmlUnmarshaller[ParamResponse] {
  override def fromXml(xml: NodeSeq) = unmarshal(xml)

  override def fromI2b2(xml: NodeSeq) = unmarshal(xml)
  
  private def unmarshal(xml: NodeSeq): ParamResponse = {
    ParamResponse(
      (xml \ "@name").text,
      (xml \ "@column").text,
      xml.text)
  }
}