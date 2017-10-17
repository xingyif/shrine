package net.shrine.protocol.query

import net.shrine.log.Loggable

import scala.xml.NodeSeq
import javax.xml.datatype.XMLGregorianCalendar
import javax.xml.datatype.DatatypeConstants
import scala.xml.Elem
import net.shrine.util.{Tries, XmlUtil, NodeSeqEnrichments, OptionEnrichments, StringEnrichments}
import net.liftweb.json._
import net.liftweb.json.JsonDSL._
import net.shrine.serialization.{ JsonMarshaller, I2b2Marshaller, XmlMarshaller, XmlUnmarshaller }
import scala.util.Try
import scala.xml.Utility
import scala.xml.Node
import scala.xml.Text

/**
 *
 * @author Clint Gilbert
 * @since Jan 25, 2012
 *
 * @see http://cbmi.med.harvard.edu
 *
 * This software is licensed under the LGPL
 * @see http://www.gnu.org/licenses/lgpl.html
 *
 * Classes to form expression trees representing Shrine queries
 */
final case class QueryDefinition(
  private val unTrimmedName: String,
  expr: Option[Expression],
  timing: Option[QueryTiming] = Some(QueryTiming.Any),
  id: Option[String] = None,
  queryType: Option[String] = None,
  constraints: Option[I2b2SubQueryConstraints] = None,
  subQueries: Seq[QueryDefinition] = Nil) extends I2b2Marshaller with XmlMarshaller with JsonMarshaller {

  //TODO: Enforce some invariant regarding the now-optional expr field?  That field can be None, but then
  //*some* of the remaining, optional fields should be filled in.  For now, I don't know what combinations
  //are valid.  -Clint Jan 7, 2015
  
  import QueryDefinition._
  import XmlUtil._
  import OptionEnrichments._

  def name = unTrimmedName.trim

  def transform(f: Expression => Expression): QueryDefinition = this.copy(expr = this.expr.map(f), subQueries = subQueries.map(_.transform(f)))

  override def toJson: JValue = ("name" -> name) ~ ("expression" -> expr.map(_.toJson))

  private def membersForEqualityAndHashing: Seq[Any] = {
    copy(unTrimmedName = name).productIterator.toIndexedSeq
  }

  override def equals(other: Any): Boolean = other match {
    case that: QueryDefinition => this.membersForEqualityAndHashing == that.membersForEqualityAndHashing
    case _ => false
  }

  override def hashCode: Int = this.membersForEqualityAndHashing.hashCode

  override def toXml: NodeSeq = stripWhitespace {
    import OptionEnrichments._

    renameRootTag(rootTagName) {
      <placeholder>
        <name>{ name }</name>
        { expr.toXml(<expr/>, _.toXml) }
        { timing.filterNot(_.isAny).toXml(<timing/>) /* Don't include 'Any' */ }
        { id.toXml(<id/>) }
        { queryType.toXml(<type/>) }
        { constraints.toXml(_.toXml) }
        { subQueries.map(_.toXml.head).map(renameRootTag(subQueryTagName)) }
      </placeholder>
    }
  }

  //TODO: Will <use_shrine> and <specificity_scale> ever change?
  override def toI2b2: NodeSeq = stripWhitespace {
    <query_definition>
      { id.toXml(<query_id/>) }
      { queryType.toXml(<query_type/>) }
      <query_name>{ name }</query_name>
      <query_timing>{ timing.getOrElse(QueryTiming.Any) }</query_timing>
      <specificity_scale>0</specificity_scale>
      <use_shrine>1</use_shrine>
      {
        /* Produce a sequence of <panel>s */
        val panelXmls = for {
          e <- expr.toSeq
          panel <- toPanels(e).map(_.toI2b2)
        } yield panel

        panelXmls.foldLeft(NodeSeq.Empty) { _ ++ _ }
      }
      { constraints.toXml(_.toI2b2) }
      { subQueries.map(_.toI2b2.head).map(renameRootTag("subquery")) }
    </query_definition>
  }
}

object QueryDefinition extends XmlUnmarshaller[Try[QueryDefinition]] with Loggable {

  //NB: For backward-compatibility.  Note that multiple apply() overloads with
  //default args are not allowed. :\
  def apply(unTrimmedName: String, expr: Expression): QueryDefinition = QueryDefinition(unTrimmedName, Option(expr))

  val rootTagName = "queryDefinition"

  val subQueryTagName = "subQuery"

  private[this] def trim(xml: NodeSeq): String = xml.text.trim

  override def fromXml(nodeSeq: NodeSeq): Try[QueryDefinition] = {
    import NodeSeqEnrichments.Strictness._

    val outerTag = Utility.trim(nodeSeq.head)

    val nameAttempt = (outerTag withChild "name").map(trim)

    val exprXmlAttempt = (outerTag withChild "expr").map(_.head.asInstanceOf[Elem])

    val innerExprXmlAttempt = exprXmlAttempt.map(_.child.head)

    for {
      name <- nameAttempt
      expr = innerExprXmlAttempt.flatMap(Expression.fromXml).toOption
      timing = QueryTiming.valueOfOrElse((outerTag \ "timing").text)(QueryTiming.Any)
      id = (outerTag \ "id").headOption.map(trim)
      queryType = (outerTag \ "type").headOption.map(trim)
      constraints = (outerTag \ I2b2SubQueryConstraints.rootTagName).headOption.flatMap(I2b2SubQueryConstraints.fromXml(_).toOption)
      subQueries = (outerTag \ subQueryTagName).flatMap(QueryDefinition.fromXml(_).toOption)
    } yield {
      QueryDefinition(name, expr, Option(timing), id, queryType, constraints, subQueries)
    }
  }

  def fromI2b2(i2b2Xml: String): Try[QueryDefinition] = {
    import StringEnrichments._

    i2b2Xml.tryToXml.flatMap(fromI2b2)
  }

  //I2b2 query definition XML => Expression (kinda sorta)
  def fromI2b2(xml: NodeSeq): Try[QueryDefinition] = {
    val outerTag = xml.head

    val name = trim(outerTag \ "query_name")

    val timing = QueryTiming.valueOf(trim(outerTag \ "query_timing"))

    def optionalTag(name: String): Option[String] = (outerTag \ name).headOption.map(trim)

    val idOption = optionalTag("query_id")

    val queryTypeOption = optionalTag("query_type")

    val panelsXml = outerTag \ "panel"

    import Tries.sequence

    val panelsAttempt = sequence(panelsXml.map(Panel.fromI2b2))

    val exprsAttempt = panelsAttempt.map(ps => ps.map(_.toExpression))

    val consolidatedExprAttempt: Try[Option[Expression]] = exprsAttempt.map {
      case es if es.isEmpty => None
      case es if es.size == 1 => Some(es.head)
      case es => Some(And(es: _*))
    }

    import NodeSeqEnrichments.Strictness._

    val constraintsOption = outerTag.withChild("subquery_constraint").flatMap(I2b2SubQueryConstraints.fromI2b2).toOption

    val subQueriesXml = (outerTag \ "subquery")

    val subQueriesAttempt = sequence(subQueriesXml.map(fromI2b2))

    for {
      consolidated <- consolidatedExprAttempt
      subQueries <- subQueriesAttempt
    } yield {
      QueryDefinition(name, consolidated.map(_.normalize), timing, idOption, queryTypeOption, constraintsOption, subQueries)
    }
  }

  private[query] def isAllTerms(exprs: Seq[Expression]) = !exprs.isEmpty && exprs.forall(_.isTerm)

  def toPanels(expr: Expression): Seq[Panel] = {
    //NB: Revisit default PanelTiming here

    def panelWithDefaults(terms: Seq[SimpleExpression], constrainedTerms: Seq[Constrained] = Nil, timing: PanelTiming = PanelTiming.Any): Panel = Panel(1, false, 1, None, None, timing, terms, constrainedTerms)

    val resultPanels = expr.normalize match {
      case t: Term => Seq(panelWithDefaults(Seq(t)))
      case constrained @ Constrained(expr, modifierOption, valueConstraintOption) => {
        Seq(panelWithDefaults(Nil, Seq(constrained)))
      }
      case WithTiming(panelTiming, expr) => toPanels(expr).map(_.withTiming(panelTiming))
      case Not(e) => toPanels(e).map(_.invert)
      case And(exprs @ _*) => exprs.flatMap(toPanels).toSeq
      case Or(exprs @ _*) => exprs match {
        case Nil => Nil
        case _ => {
          //Or-expressions must be comprised of terms only, dues to limitations in i2b2's query representation
          require(isAllTerms(exprs), "Or-expressions must be comprised of Terms *only*.  Sorry.")

          val constraints = exprs.collect { case c: Constrained => c }

          //Terms only :\
          Seq(panelWithDefaults(exprs.collect { case um: SimpleExpression => um }, constraints))
        }
      }
      case DateBounded(start, end, e) => {
        toPanels(e).map(_.withStart(start)).map(_.withEnd(end))
      }
      case OccuranceLimited(min, e) => toPanels(e).map(_.withMinOccurrences(min))
      case q: Query => {
        //TODO: implement for query-in-query

        val message = s"Couldn't turn query-in-query expressions '$q' into i2b2 panels"

        error(message)

        sys.error(message)
      }
      case unexpected => {
        //We should never get here, but fail loudly just in case.  Also fixes compiler
        //warning about unhandled cases for abstract, non-instantiatable Expression subtypes
        val message = s"Unexpected expression '$unexpected' can't be turned into i2b2 panels"

        error(message)

        sys.error(message)
      }
    }

    //Assign indicies; i2b2 indicies start from 1 
    resultPanels.size match {
      case 0 | 1 => resultPanels
      case _ => resultPanels.zipWithIndex.map {
        case (panel, index) => panel.copy(number = index + 1)
      }
    }
  }
}