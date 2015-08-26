package net.shrine.protocol

import xml.NodeSeq
import net.shrine.util.XmlUtil
import net.shrine.serialization.{ I2b2Unmarshaller, XmlUnmarshaller }

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
final case class PatientResponse(
  val patientId: String,
  val params: Seq[ParamResponse]) extends ShrineResponse {

  override def i2b2MessageBody = marshal(_.i2b2MessageBody)

  override def toXml = marshal(_.toXml)

  private def marshal(marshalParam: ParamResponse => NodeSeq): NodeSeq = {
    XmlUtil.stripWhitespace(
      <patient>
        <patient_id>
          { patientId }
        </patient_id>{ params.map(marshalParam) }
      </patient>)
  }
}

object PatientResponse extends I2b2Unmarshaller[PatientResponse] with XmlUnmarshaller[PatientResponse] {
  override def fromXml(xml: NodeSeq) = unmarshal(ParamResponse.fromXml)(xml)

  override def fromI2b2(xml: NodeSeq) = unmarshal(ParamResponse.fromI2b2)(xml)
  
  private def unmarshal(unmarshalParam: NodeSeq => ParamResponse)(xml: NodeSeq): PatientResponse = {
    PatientResponse(
      (xml \ "patient_id").text,
      (xml \ "param").map(unmarshalParam))
  }
}