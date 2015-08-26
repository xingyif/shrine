package net.shrine.protocol.query

import javax.xml.datatype.XMLGregorianCalendar
import scala.xml.NodeSeq
import net.shrine.util.XmlUtil
import net.liftweb.json.JsonDSL._
import net.shrine.serialization.{ JsonUnmarshaller, I2b2Marshaller }
import net.liftweb.json.JsonAST._
import scala.None
import net.liftweb.json.DefaultFormats
import scala.util.Try
import net.shrine.util.XmlDateHelper
import scala.util.Success
import net.shrine.util.OptionEnrichments
import net.shrine.util.NodeSeqEnrichments

/**
 *
 * @author Clint Gilbert
 * @date Jan 25, 2012
 *
 * @link http://cbmi.med.harvard.edu
 *
 * This software is licensed under the LGPL
 * @link http://www.gnu.org/licenses/lgpl.html
 *
 */
final case class Panel(
  number: Int,
  inverted: Boolean,
  minOccurrences: Int,
  start: Option[XMLGregorianCalendar],
  end: Option[XMLGregorianCalendar],
  timing: PanelTiming,
  terms: Seq[SimpleExpression],
  termsWithConstraints: Seq[Constrained] = Seq.empty) extends I2b2Marshaller {

  require(!terms.isEmpty || !termsWithConstraints.isEmpty, s"Panels must have at least one constrained or unconstrained term: plain terms: $terms ; modified terms: $termsWithConstraints")

  import Panel._

  private[query] def withTerms(newTerms: Seq[SimpleExpression]) = copy(terms = newTerms)
  private[query] def withConstrainedTerms(newTerms: Seq[Constrained]) = copy(termsWithConstraints = newTerms)

  private[query] def withOnlyTerms(newTerms: Seq[SimpleExpression]) = withTerms(newTerms).withConstrainedTerms(Nil)
  private[query] def withOnlyConstrainedTerms(newTerms: Seq[Constrained]) = withConstrainedTerms(newTerms).withTerms(Nil)

  def invert = this.copy(inverted = !this.inverted)

  def withStart(startDate: Option[XMLGregorianCalendar]) = this.copy(start = startDate)

  def withEnd(endDate: Option[XMLGregorianCalendar]) = this.copy(end = endDate)

  def withMinOccurrences(min: Int) = this.copy(minOccurrences = min)

  def withTiming(panelTiming: PanelTiming) = this.copy(timing = panelTiming)

  private def allTerms: Seq[(SimpleExpression, Option[Modifiers], Option[ValueConstraint])] = {
    val termsWithNoConstraints = terms.map(t => (t, None, None))
    
    val termsWithSomeConstraints = termsWithConstraints.map(_.toTuple)

    termsWithNoConstraints ++ termsWithSomeConstraints
  }

  //TODO: Do dates have to be in UTC?  Ones sent by web client seem to be
  //TODO: <date_{from,to}> vs <panel_date_{from,to}>?? web client uses both
  //TODO: <class>ENC</class> on items: is it ever anything else?
  //TODO: Are <item_name>, <tooltip>, <item_icon>, and <item_is_synonym> on items needed?
  override def toI2b2: NodeSeq = XmlUtil.stripWhitespace {
    import OptionEnrichments._

    <panel>
      <panel_number>{ number }</panel_number>
      { start.map(s => <panel_date_from>{ s.toString }</panel_date_from>).getOrElse(Nil) }
      { end.map(e => <panel_date_to>{ e.toString }</panel_date_to>).getOrElse(Nil) }
      <invert>{ if (inverted) 1 else 0 }</invert>
      <panel_timing>{ timing }</panel_timing>
      <total_item_occurrences>{ minOccurrences }</total_item_occurrences>
      {
        allTerms.map {
          case (expr, modifierOption, constraintOption) =>
            <item>
              <hlevel>{ expr.computeHLevel.getOrElse(0) }</hlevel>
              <item_name>{ expr.value }</item_name>
              <item_key>{ expr.value }</item_key>
              <tooltip>{ expr.value }</tooltip>
              <class>ENC</class>
              <constrain_by_date>
                { start.map(s => <date_from>{ s.toString }</date_from>).orNull }
                { end.map(e => <date_to>{ e.toString }</date_to>).orNull }
              </constrain_by_date>
              { modifierOption.toXml(_.toI2b2) }
              { constraintOption.toXml(_.toI2b2) }
              <item_icon>LA</item_icon>
              <item_is_synonym>false</item_is_synonym>
            </item>
        }
      }
    </panel>
  }

  def toExpression: Expression = {
    def addTiming(expr: Expression) = if (timing.isAny) expr else WithTiming(timing, expr)

    def limit(expr: Expression) = if (minOccurrences != 0) OccuranceLimited(minOccurrences, expr) else expr

    def dateBound(expr: Expression): Expression = {
      if (start.isDefined || end.isDefined) DateBounded(start, end, expr) else expr
    }

    def negate(expr: Expression) = if (inverted) Not(expr) else expr

    val exprs: Seq[Expression] = {
      allTerms.collect {
        case (expr: Term, modifiersOption, valueConstraintOption) => Constrained(expr, modifiersOption, valueConstraintOption)
        case (expr, None, None) => expr
      }
    }
    
    addTiming(limit(dateBound(negate(Or(exprs: _*))))).normalize
  }
}

object Panel {

  def fromI2b2(xml: NodeSeq): Try[Panel] = {

    import XmlUtil.{ trim, toInt }

    def toXmlGcOption(dateXml: NodeSeq): Option[XMLGregorianCalendar] = {
      XmlDateHelper.parseXmlTime(trim(dateXml)).toOption
    }

    import NodeSeqEnrichments.Strictness._

    for {
      outerTag <- Try(xml.head)
      number <- (outerTag withChild "panel_number").map(toInt)
      inverted <- (outerTag withChild "invert").map(toInt).map(_ == 1)
      minOccurrences <- (outerTag withChild "total_item_occurrences").map(toInt)
      start = toXmlGcOption(outerTag \ "panel_date_from")
      end = toXmlGcOption(outerTag \ "panel_date_to")
      itemXmls = (outerTag \ "item")
      timing = PanelTiming.valueOfOrElse(trim(outerTag \ "panel_timing"))(PanelTiming.Any)
    } yield {
      val termsToConstraints: Seq[(SimpleExpression, Option[Constrained])] = for {
        itemXml <- itemXmls
        termOrQuery: SimpleExpression = XmlUtil.trim(itemXml \ "item_key") match {
          case Query.prefixRegex(id) => Query(id)
          case x => Term(x)
        }
        modifierOption = Modifiers.fromI2b2(itemXml \ "constrain_by_modifier").toOption
        valueConstraintOption = ValueConstraint.fromI2b2(itemXml \ "constrain_by_value").toOption
        termOption = Option(termOrQuery).collect { case t: Term => t }
        constrainedOption = {
          if(modifierOption.isEmpty && valueConstraintOption.isEmpty) { None } 
          else { termOption.map(t => Constrained(t, modifierOption, valueConstraintOption)) } 
        }
      } yield {
        termOrQuery -> constrainedOption
      }

      val termsWithNoConstraints: Seq[SimpleExpression] = termsToConstraints.collect { case (expr, None) => expr }.toSeq
      val termsWithSomeConstraints: Seq[Constrained] = termsToConstraints.collect { case (_, Some(constrained)) => constrained }.toSeq

      Panel(number, inverted, minOccurrences, start, end, timing, termsWithNoConstraints, termsWithSomeConstraints)
    }
  }
}