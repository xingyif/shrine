package net.shrine.protocol

import xml.NodeSeq
import net.shrine.util.XmlUtil
import net.shrine.serialization.{ I2b2Unmarshaller, XmlUnmarshaller }

/**
 * @author Justin Quan
 * @date 8/21/11
 * @link http://chip.org
 *       <p/>
 *       NOTICE: This software comes with NO guarantees whatsoever and is
 *       licensed as Lgpl Open Source
 * @link http://www.gnu.org/licenses/lgpl.html
 *
 * NB: Includes custom equals() and hashCode; the compiler-generated ones were not working as expected with the
 * observation field, due to its XML-ness
 */
final case class ObservationResponse(
  val eventIdSource: Option[String],
  val eventId: String,
  val patientIdSource: Option[String],
  val patientId: String,
  val conceptCodeName: Option[String],
  val conceptCode: Option[String],
  val observerCodeSource: Option[String],
  val observerCode: String,
  val startDate: String,
  val modifierCode: Option[String],
  val valueTypeCode: String,
  val tvalChar: Option[String],
  val nvalNum: Option[String],
  val valueFlagCode: Option[String],
  val unitsCode: Option[String],
  val endDate: Option[String],
  val locationCodeName: Option[String],
  val locationCode: Option[String],
  val params: Seq[ParamResponse]) {

  def toI2b2 = XmlUtil.stripWhitespace(
    <observation>
      <event_id source={ eventIdSource.getOrElse("") }>{ eventId }</event_id>
      <patient_id source={ patientIdSource.getOrElse("") }>{ patientId }</patient_id>
      { conceptCode.map(x => <concept_cd name={ conceptCodeName.getOrElse("") }>{ x }</concept_cd>).getOrElse(<concept_cd/>) }
      <observer_cd source={ observerCodeSource.getOrElse("") }>{ observerCode }</observer_cd>
      <start_date>{ startDate }</start_date>
      { modifierCode.map(x => <modifier_cd>{ x }</modifier_cd>).getOrElse(<modifier_cd/>) }
      <valuetype_cd>{ valueTypeCode }</valuetype_cd>
      { nvalNum.map(x => <nval_num>{ x }</nval_num>).getOrElse(<nval_num/>) }
      { valueFlagCode.map(x => <valueflag_cd>{ x }</valueflag_cd>).getOrElse(<valueflag_cd/>) }
      { endDate.map(x => <end_date>{ x }</end_date>).getOrElse(<end_date/>) }
      { locationCode.map(x => <location_cd name={ locationCodeName.getOrElse("") }>{ x }</location_cd>).getOrElse(<location_cd/>) }
      { params.map(_.i2b2MessageBody) }
    </observation>)

  def toXml = XmlUtil.stripWhitespace(
    <observation>
      <event_id source={ eventIdSource.getOrElse("") }>{ eventId }</event_id>
      <patient_id source={ patientIdSource.getOrElse("") }>{ patientId }</patient_id>
      { conceptCode.map(x => <concept_cd name={ conceptCodeName.getOrElse("") }>{ x }</concept_cd>).getOrElse(<concept_cd/>) }
      <observer_cd source={ observerCodeSource.getOrElse("") }>{ observerCode }</observer_cd>
      <start_date>{ startDate }</start_date>
      { modifierCode.map(x => <modifier_cd>{ x }</modifier_cd>).getOrElse(<modifier_cd/>) }
      <valuetype_cd>{ valueTypeCode }</valuetype_cd>
      { nvalNum.map(x => <nval_num>{ x }</nval_num>).getOrElse(<nval_num/>) }
      { tvalChar.map(x => <tval_char>{ x }</tval_char>).getOrElse(<tval_char/>) }
      { valueFlagCode.map(x => <valueflag_cd>{ x }</valueflag_cd>).getOrElse(<valueflag_cd/>) }
      { endDate.map(x => <end_date>{ x }</end_date>).getOrElse(<end_date/>) }
      { locationCode.map(x => <location_cd name={ locationCodeName.getOrElse("") }>{ x }</location_cd>).getOrElse(<location_cd/>) }
      { params.map(_.toXml) }
    </observation>)

  override def canEqual(other: Any): Boolean = other.isInstanceOf[ObservationResponse]

  //NB: Compares structurally, but turns observation fields to Strings, since XML comparisons are unreliable
  override def equals(other: Any): Boolean = {
    other match {
      case that: ObservationResponse => (that canEqual this) &&
        (eventIdSource == that.eventIdSource) &&
        (eventId == that.eventId) &&
        (patientIdSource == that.patientIdSource) &&
        (patientId == that.patientId) &&
        (conceptCodeName == that.conceptCodeName) &&
        (conceptCode == that.conceptCode) &&
        (observerCodeSource == that.observerCodeSource) &&
        (observerCode == that.observerCode) &&
        (startDate == that.startDate) &&
        (modifierCode == that.modifierCode) &&
        (valueTypeCode == that.valueTypeCode) &&
        (tvalChar == that.tvalChar) &&
        (nvalNum == that.nvalNum) &&
        (valueFlagCode == that.valueFlagCode) &&
        (endDate == that.endDate) &&
        (locationCodeName == that.locationCodeName) &&
        (locationCode == that.locationCode) &&
        (unitsCode == that.unitsCode) &&
        (params == that.params)
      case _ => false
    }
  }

  //NB: Turns observation field from a NodeSeq to a String, to match equals()
  override def hashCode: Int = 41 * (41 + eventIdSource.hashCode +
    patientIdSource.hashCode +
    patientId.hashCode + conceptCodeName.hashCode +
    conceptCode.hashCode +
    observerCodeSource.hashCode +
    observerCode.hashCode +
    startDate.hashCode +
    modifierCode.hashCode +
    valueTypeCode.hashCode +
    nvalNum.hashCode +
    valueFlagCode.hashCode +
    endDate.hashCode +
    locationCodeName.hashCode +
    tvalChar.hashCode +
    unitsCode.hashCode +
    params.hashCode)
}

object ObservationResponse extends I2b2Unmarshaller[ObservationResponse] with XmlUnmarshaller[ObservationResponse] {
  override def fromXml(nodeSeq: NodeSeq) = {

    def checkEmptyString(s: String): Option[String] = Option(s).filterNot(_.isEmpty)

    new ObservationResponse(
      checkEmptyString((nodeSeq \ "event_id" \ "@source").text),
      (nodeSeq \ "event_id").text,
      checkEmptyString((nodeSeq \ "patient_id" \ "@source").text),
      (nodeSeq \ "patient_id").text,
      checkEmptyString((nodeSeq \ "concept_cd" \ "@name").text),
      checkEmptyString((nodeSeq \ "concept_cd").text),
      //yes it's 'soruce' in i2b2 1.5
      checkEmptyString((nodeSeq \ "observer_cd" \ "@soruce").text),
      (nodeSeq \ "observer_cd").text,
      (nodeSeq \ "start_date").text,
      checkEmptyString((nodeSeq \ "modifier_cd").text),
      (nodeSeq \ "valuetype_cd").text,
      checkEmptyString((nodeSeq \ "tval_char").text),
      checkEmptyString((nodeSeq \ "nval_num").text),
      checkEmptyString((nodeSeq \ "valueflag_cd").text),
      checkEmptyString((nodeSeq \ "units_cd").text),
      checkEmptyString((nodeSeq \ "end_date").text),
      checkEmptyString((nodeSeq \ "location_cd" \ "@name").text),
      checkEmptyString((nodeSeq \ "location_cd").text),
      (nodeSeq \ "param").map(ParamResponse.fromXml))
  }

  override def fromI2b2(nodeSeq: NodeSeq) = fromXml(nodeSeq)
}
