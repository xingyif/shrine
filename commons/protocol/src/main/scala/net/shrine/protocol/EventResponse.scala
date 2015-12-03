package net.shrine.protocol

import xml.NodeSeq
import javax.xml.datatype.XMLGregorianCalendar
import net.shrine.util.XmlUtil
import net.shrine.serialization.{ I2b2Unmarshaller, XmlUnmarshaller }
import net.shrine.util.XmlDateHelper
import scala.util.Try

/**
 * @author Justin Quan
 * @date 10/27/11
 * @link http://chip.org
 *       <p/>
 *       NOTICE: This software comes with NO guarantees whatsoever and is
 *       licensed as Lgpl Open Source
 * @link http://www.gnu.org/licenses/lgpl.html
 */
final case class EventResponse(
  val eventId: String,
  val patientId: String,
  val startDate: Option[XMLGregorianCalendar],
  val endDate: Option[XMLGregorianCalendar],
  val params: Seq[ParamResponse]) extends ShrineResponse {

  override def i2b2MessageBody = serialize(_.i2b2MessageBody)

  override def toXml = serialize(_.toXml)

  private def serialize(serializeParamResponse: ParamResponse => NodeSeq) = XmlUtil.stripWhitespace(
    <event>
      <event_id>
        { eventId }
      </event_id>
      <patient_id>
        { patientId }
      </patient_id>
      { params.map(serializeParamResponse) }
      { startDate.map(x => <start_date>{ x }</start_date>).orNull }
      { endDate.map(x => <end_date>{ x }</end_date>).orNull }
    </event>)
}

object EventResponse extends I2b2Unmarshaller[EventResponse] with XmlUnmarshaller[EventResponse] {
  override def fromXml(nodeSeq: NodeSeq) = unmarshal(nodeSeq)

  override def fromI2b2(nodeSeq: NodeSeq) = unmarshal(nodeSeq)

  private def unmarshal(nodeSeq: NodeSeq): EventResponse = {
    val startDateOption = XmlDateHelper.parseXmlTime((nodeSeq \ "start_date").text).toOption

    val endDateOption = XmlDateHelper.parseXmlTime((nodeSeq \ "end_date").text).toOption

    EventResponse(
      (nodeSeq \ "event_id").text,
      (nodeSeq \ "patient_id").text,
      startDateOption,
      endDateOption,
      (nodeSeq \ "param").map { p =>
        ParamResponse(
          (p \ "@name").text,
          (p \ "@column").text,
          p.text)
      })
  }
}