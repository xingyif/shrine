package net.shrine.protocol.query

import scala.xml.NodeSeq
import scala.util.Try
import net.shrine.util.NodeSeqEnrichments
import net.liftweb.json.JsonAST._
import net.liftweb.json.JsonDSL._
import scala.util.Success
import scala.util.Failure
import net.shrine.util.XmlUtil
import net.shrine.util.OptionEnrichments

/**
 * @author clint
 * @date Dec 1, 2014
 */
final case class ValueConstraint(valueType: String, unit: Option[String], operator: String, value: String) {
  import OptionEnrichments._
  
  def toI2b2: NodeSeq = XmlUtil.stripWhitespace {
    <constrain_by_value>
      <value_type>{ valueType }</value_type>
      { unit.toXml(<value_unit_of_measure/>) }
      <value_operator>{ operator }</value_operator>
      <value_constraint>{ value }</value_constraint>
    </constrain_by_value>
  }

  def toXml: NodeSeq = XmlUtil.stripWhitespace {
    <valueConstraint>
      <valueType>{ valueType }</valueType>
      { unit.toXml(<unitOfMeasure/>) }
      <operator>{ operator }</operator>
      <value>{ value }</value>
    </valueConstraint>
  }

  def toJson: JValue = {
    ("valueType" -> valueType) ~ ("unitOfMeasure" -> unit) ~ ("operator" -> operator) ~ ("value" -> value)
  }
}

object ValueConstraint {
  def fromI2b2(xml: NodeSeq): Try[ValueConstraint] = unmarshalXml(xml, "value_type", "value_unit_of_measure", "value_operator", "value_constraint")

  def fromXml(xml: NodeSeq): Try[ValueConstraint] = unmarshalXml(xml, "valueType", "unitOfMeasure", "operator", "value")

  private def unmarshalXml(xml: NodeSeq, valueTypeTagName: String, unitTagName: String, operatorTagName: String, valueTagName: String): Try[ValueConstraint] = {
    import NodeSeqEnrichments.Strictness._

    def tagValue(tagName: String) = xml.withChild(tagName).map(XmlUtil.trim)
    
    for {
      valueType <- tagValue(valueTypeTagName)
      unit = tagValue(unitTagName).toOption
      operator <- tagValue(operatorTagName)
      value <- tagValue(valueTagName)
    } yield ValueConstraint(valueType, unit, operator, value)
  }

  def fromJson(json: JValue): Try[ValueConstraint] = {
    import JsonEnrichments._

    for {
      valueType <- json.withChildString("valueType")
      unit = json.withChildString("unitOfMeasure").toOption
      operator <- json.withChildString("operator")
      value <- json.withChildString("value")
    } yield ValueConstraint(valueType, unit, operator, value)
  }
}
