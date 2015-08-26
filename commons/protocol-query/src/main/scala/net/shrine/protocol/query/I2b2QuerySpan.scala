package net.shrine.protocol.query

import net.shrine.serialization.I2b2Marshaller
import scala.xml.NodeSeq
import net.shrine.util.XmlUtil
import net.shrine.util.NodeSeqEnrichments
import scala.util.Try
import net.shrine.serialization.XmlMarshaller

/**
 * @author clint
 * @date Jan 7, 2015
 * 
 * NB: Tests for this class covered by I2b2SubQueryConstraintsTest
 */
final case class I2b2QuerySpan(operator: String, value: String, unit: String) extends I2b2Marshaller with XmlMarshaller {
  override def toI2b2: NodeSeq = XmlUtil.stripWhitespace {
    <span>
      <operator>{ operator }</operator>
      <span_value>{ value }</span_value>
      <units>{ unit }</units>
    </span>
  }

  import I2b2QuerySpan._

  def toXml: NodeSeq = XmlUtil.stripWhitespace {
    XmlUtil.renameRootTag(rootTagName) {
      <placeholder>
        <operator>{ operator }</operator>
        <value>{ value }</value>
        <units>{ unit }</units>
      </placeholder>
    }
  }
}

object I2b2QuerySpan {
  val rootTagName = "i2b2QuerySpan"
  
  def fromXml(xml: NodeSeq): Try[I2b2QuerySpan] = parse(xml, "operator", "value", "units")

  def fromI2b2(xml: NodeSeq): Try[I2b2QuerySpan] = parse(xml, "operator", "span_value", "units")
  
  private def parse(xml: NodeSeq, operatorTagName: String, valueTagName: String, unitsTagName: String): Try[I2b2QuerySpan] = {
    import NodeSeqEnrichments.Strictness._
    import XmlUtil.trim

    for {
      operator <- xml.withChild(operatorTagName).map(trim)
      value <- xml.withChild(valueTagName).map(trim)
      units <- xml.withChild(unitsTagName).map(trim)
    } yield {
      I2b2QuerySpan(operator, value, units)
    }
  }
}