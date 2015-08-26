package net.shrine.protocol.query

import net.shrine.serialization.I2b2Marshaller
import scala.xml.NodeSeq
import net.shrine.util.XmlUtil
import net.shrine.util.SEnum
import scala.util.Try
import net.shrine.util.NodeSeqEnrichments
import net.shrine.serialization.XmlMarshaller
import net.shrine.util.OptionEnrichments

/**
 * @author clint
 * @date Sep 25, 2014
 *
 * A class to handle parameters to I2b2's notion of a temporal query
 * (achieved by choosing 'define sequence of events' in the legacy web client)
 */
final case class I2b2SubQueryConstraints(operator: String, first: I2b2SubQueryConstraint, second: I2b2SubQueryConstraint, span: Option[I2b2QuerySpan]) extends I2b2Marshaller with XmlMarshaller {
  
  override def toI2b2: NodeSeq = XmlUtil.stripWhitespace {
    def subQueryToI2b2(name: String, sq: I2b2SubQueryConstraint) = XmlUtil.renameRootTag(name)(sq.toI2b2.head)

    <subquery_constraint>
      { subQueryToI2b2("first_query", first) }
      <operator>{ operator }</operator>
      { subQueryToI2b2("second_query", second) }
      { span.map(_.toI2b2).orNull }
    </subquery_constraint>
  }

  import I2b2SubQueryConstraints._

  override def toXml: NodeSeq = XmlUtil.stripWhitespace {
    XmlUtil.renameRootTag(rootTagName) {
      <placeholder>
        { first.toXml(0) }
        <operator>{ operator }</operator>
        { second.toXml(1) }
        { span.map(_.toXml).orNull }
      </placeholder>
    }
  }
}

object I2b2SubQueryConstraints {
  val rootTagName = "i2b2SubQueryConstraints"

  import NodeSeqEnrichments.Strictness._
  
  private[this] def parseSpan(xml: NodeSeq, tagName: String, parseFn: NodeSeq => Try[I2b2QuerySpan]): Option[I2b2QuerySpan] = {
    xml.withChild(tagName).flatMap(parseFn).toOption
  }
  
  def fromXml(xml: NodeSeq): Try[I2b2SubQueryConstraints] = {
    def subQueryXmlWithIndex(idx: Int): Try[NodeSeq] = {
      import I2b2SubQueryConstraint.{ rootTagName => subQueryTagName }
      
      def indexMatches(x: NodeSeq): Boolean = (x \ "@index").text.trim == idx.toString
      
      Try((xml \ subQueryTagName).filter(indexMatches).head)
    }
    
    for {
      operator <- xml.withChild("operator").map(_.text)
      first <- subQueryXmlWithIndex(0).flatMap(I2b2SubQueryConstraint.fromXml)
      second <- subQueryXmlWithIndex(1).flatMap(I2b2SubQueryConstraint.fromXml)
      span = parseSpan(xml, I2b2QuerySpan.rootTagName, I2b2QuerySpan.fromXml)
    } yield {
      I2b2SubQueryConstraints(operator, first, second, span)
    }
  }
  
  def fromI2b2(xml: NodeSeq): Try[I2b2SubQueryConstraints] = {
    for {
      operator <- xml.withChild("operator").map(_.text)
      first <- xml.withChild("first_query").flatMap(I2b2SubQueryConstraint.fromI2b2)
      second <- xml.withChild("second_query").flatMap(I2b2SubQueryConstraint.fromI2b2)
      span = parseSpan(xml, "span", I2b2QuerySpan.fromI2b2)
    } yield {
      I2b2SubQueryConstraints(operator, first, second, span)
    }
  }
}

