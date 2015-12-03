package net.shrine.protocol.query

import net.shrine.log.Loggable

import scala.xml.NodeSeq
import javax.xml.datatype.XMLGregorianCalendar
import net.shrine.util.{Tries, XmlUtil, XmlDateHelper, NodeSeqEnrichments, OptionEnrichments}
import net.liftweb.json.JsonDSL._
import net.shrine.serialization.{ JsonUnmarshaller, JsonMarshaller, XmlMarshaller, XmlUnmarshaller }
import net.liftweb.json.JsonAST._
import scala.util.Try
import scala.util.Failure
import scala.util.Success
import scala.xml.Utility

import Expression.mappingFailure

/**
 *
 * @author Clint Gilbert
 * @since Jan 24, 2012
 *
 * @see http://cbmi.med.harvard.edu
 *
 * This software is licensed under the LGPL
 * @see http://www.gnu.org/licenses/lgpl.html
 *
 * Classes to form expression trees representing Shrine queries
 */
sealed trait Expression extends XmlMarshaller with JsonMarshaller {
  def normalize: Expression = this

  def hasDirectI2b2Representation: Boolean

  def toExecutionPlan: ExecutionPlan

  def isTerm: Boolean = false

  //NB: See ExpressionTranslatorTest.{testTranslate*,testTryTranslate}
  def translate(lookup: String => Set[String]): Try[Expression]
}

object Expression extends XmlUnmarshaller[Try[Expression]] with JsonUnmarshaller[Try[Expression]] {

  private def to[C <: ComposeableExpression[C]](make: (Expression*) => C): Seq[Expression] => C = make(_: _*)

  private val toOr = to(Or)
  private val toAnd = to(And)

  import Tries.sequence

  def fromJson(json: JValue): Try[Expression] = {
    def dateFromJson(json: JValue): Try[XMLGregorianCalendar] = {
      json match {
        case JString(value) => XmlDateHelper.parseXmlTime(value)
        case _ => Failure(new Exception(s"Cannot parse json date $json")) //TODO some sort of unmarshalling exception
      }
    }

    import JsonEnrichments._

    json.children.head match {
      case JField("term", JString(value)) => Try(Term(value))
      case JField("constrainedTerm", constrainedTermJson) => {
        for {
          termValue <- constrainedTermJson.withChildString("term")
          modifiers = constrainedTermJson.withChild("modifiers", (j: JValue) => Modifiers.fromJson(j)).toOption
          constraint = constrainedTermJson.withChild("valueConstraint", (j: JValue) => ValueConstraint.fromJson(j)).toOption
        } yield Constrained(Term(termValue), modifiers, constraint)
      }
      case JField("query", JString(value)) => Try(Query(value))
      case JField("not", value) => fromJson(value).map(Not)
      case JField("and", value) => sequence(value.children.map(fromJson)).map(toAnd)
      case JField("or", value) => sequence(value.children.map(fromJson)).map(toOr)
      case JField("dateBounded", value) => {
        for {
          expr <- fromJson(value \ "expression")
          start = dateFromJson(value \ "start").toOption
          end = dateFromJson(value \ "end").toOption
        } yield DateBounded(start, end, expr)
      }
      case JField("occurs", value) => {
        val min = Try((value \ "min") match {
          case JInt(x) => x.intValue
          case x => throw new Exception(s"Cannot parse json: '$x'") //TODO some sort of unmarshalling exception
        })

        for {
          expr <- fromJson(value \ "expression")
          m <- min
        } yield OccuranceLimited(m, expr)
      }
      case x => Failure(new Exception(s"Cannot parse json: '$x'")) //TODO some sort of unmarshalling exception
    }
  }

  def fromXml(nodeSeq: NodeSeq): Try[Expression] = {
    def dateFromXml(dateString: String): Option[XMLGregorianCalendar] = {
      XmlDateHelper.parseXmlTime(dateString).toOption
    }

    if (nodeSeq.isEmpty) { Try(Or()) }
    else {
      import NodeSeqEnrichments.Strictness._

      val outerTag = Utility.trim(nodeSeq.head)

      val childTags = outerTag.child

      outerTag.label match {
        case "term" => Try(Term(outerTag.text))
        case "withTiming" => {
          for {
            timing <- outerTag.withChild("timing").map(XmlUtil.trim).flatMap(PanelTiming.tryValueOf)
            expr <- outerTag.withChild("expr").map(_.head.child.head).flatMap(Expression.fromXml)
          } yield WithTiming(timing, expr)
        }
        case "constrainedTerm" => {
          for {
            termValue <- outerTag.withChild("term").map(XmlUtil.trim)
            modifiers = outerTag.withChild("modifier").flatMap(Modifiers.fromXml).toOption
            constraint = outerTag.withChild("valueConstraint").flatMap(ValueConstraint.fromXml).toOption
          } yield {
            Constrained(Term(termValue), modifiers, constraint)
          }
        }
        case "query" => Try(Query(XmlUtil.trim(outerTag)))
        //childTags.head because only one child expr of <not> is allowed
        case "not" => fromXml(outerTag \ "_").map(Not)
        case "and" => {
          sequence(childTags.map(fromXml)).map(toAnd)
        }
        case "or" => {
          sequence(childTags.map(fromXml)).map(toOr)
        }
        case "dateBounded" => {
          for {
            //drop(2) to lose <start> and <end>
            //childTags.drop(2).head because only one child expr of <dateBounded> is allowed
            expr <- fromXml(childTags.drop(2).head)
            start = dateFromXml(XmlUtil.trim(nodeSeq \ "start"))
            end = dateFromXml(XmlUtil.trim(nodeSeq \ "end"))
          } yield DateBounded(start, end, expr)
        }
        case "occurs" => {
          for {
            min <- nodeSeq.withChild("min").map(XmlUtil.toInt)
            //drop(1) to lose <min>
            //childTags.drop(2).head because only one child expr of <occurs> is allowed
            expr <- fromXml(childTags.drop(1).head)
          } yield OccuranceLimited(min, expr)
        }
        case _ => Failure(new Exception(s"Cannot parse xml: '${nodeSeq.toString}'")) //TODO some sort of unmarshalling exception
      }
    }
  }

  def mappingFailure[T](message: String): Failure[T] = Failure(new MappingException(message))
}

trait HasSimpleRepresentation { self: Expression =>
  override def hasDirectI2b2Representation = true

  override def toExecutionPlan = SimplePlan(this)
}

trait SimpleExpression extends Expression with HasSimpleRepresentation {
  def value: String

  def computeHLevel: Try[Int]
}

trait KnowsOwnType {
  type MyType <: Expression
}

trait MappableExpression { self: Expression with KnowsOwnType =>
  def map(f: Expression => Expression): MyType
}

trait HasSingleSubExpression { self: Expression with KnowsOwnType =>
  val expr: Expression

  def withExpr(newExpr: Expression): MyType

  override def translate(lookup: String => Set[String]): Try[Expression] = expr.translate(lookup).map(withExpr)
}

trait HasSubExpressions { self: Expression =>
  val exprs: Seq[Expression]
}

trait HasHLevel { self: SimpleExpression =>
  override def computeHLevel: Try[Int] = {
    //Super-dumb way: calculate nesting level by dropping prefix and counting \'s
    Try(value.drop("\\\\SHRINE\\SHRINE\\".length).count(_ == '\\'))
  }
}

//NOTE - refactoring the field name value will break json deserialization for this case class
final case class Term(override val value: String) extends Expression with SimpleExpression with HasHLevel with HasSimpleRepresentation with Loggable {
  override def toXml: NodeSeq = XmlUtil.stripWhitespace(<term>{ value }</term>)

  override def toJson: JValue = ("term" -> value)

  override def isTerm = true

  override def translate(lookup: String => Set[String]): Try[Expression] = {
    val localTerms = lookup(value)

    val result: Try[Expression] = localTerms.size match {
      case 0 => mappingFailure(s"No local terms mapped to '$value'")
      case 1 => Try(if (localTerms.head == value) this else Term(localTerms.head))
      case _ => Try(Or(localTerms.map(Term).toSeq: _*))
    }

    debug(s"Translated: $this to $result")

    result
  }
}

final case class Query(localMasterId: String) extends Expression with SimpleExpression with HasSimpleRepresentation {
  override val value = s"${Query.prefix}$localMasterId"

  override def toXml: NodeSeq = XmlUtil.stripWhitespace(<query>{ localMasterId }</query>)

  override def toJson: JValue = ("query" -> localMasterId)

  override def computeHLevel = Success(0)

  override def translate(lookup: String => Set[String]): Try[Expression] = Success(this)
}

object Query {
  val prefix = "masterid:"

  val prefixRegex = (s"^$prefix(.+?)").r

  def fromString(value: String): Option[Query] = value match {
    case null => None
    case prefixRegex(masterId) => Some(Query(masterId))
    case _ => None
  }
}

final case class Constrained(term: Term, modifiers: Option[Modifiers], valueConstraint: Option[ValueConstraint]) extends Expression with HasSimpleRepresentation with Loggable {
  def toTuple: (Term, Option[Modifiers], Option[ValueConstraint]) = (term, modifiers, valueConstraint)

  override def toXml: NodeSeq = XmlUtil.stripWhitespace {
    <constrainedTerm>
      { term.toXml }
      { modifiers.map(_.toXml).orNull }
      { valueConstraint.map(_.toXml).orNull }
    </constrainedTerm>
  }

  override def toJson: JValue = {
    ("constrainedTerm" -> {
      ("term" -> term.value) ~
        ("modifiers" -> modifiers.map(_.toJson)) ~
        ("valueConstraint" -> valueConstraint.map(_.toJson))
    })
  }

  override def normalize: Expression = {
    if (modifiers.isEmpty && valueConstraint.isEmpty) { term.normalize }
    else { this }
  }

  override def isTerm = true

  def withTerm(newTerm: Term): Constrained = copy(term = newTerm)

  def withModifiers(newMods: Modifiers): Constrained = copy(modifiers = Option(newMods))
  def withModifiers(newMods: Option[Modifiers]): Constrained = copy(modifiers = newMods)

  def withValueConstraint(newConstraint: ValueConstraint): Constrained = copy(valueConstraint = Option(newConstraint))

  override def translate(lookup: String => Set[String]): Try[Expression] = {
    def translateModifierKey(key: String): Try[Expression] = {
      Term(key).translate(lookup) match {
        case s @ Success(_) => s
        case _ => mappingFailure(s"Couldn't map modifier key '$key'; it must be mapped to a single local term")
      }
    }

    def constrainTranslatedExpression(translatedExpr: Expression, translatedModifiersToApply: Option[Modifiers]): Try[Expression] = {
      def makeExprFrom(translatedTerm: Term): Constrained = this.withTerm(translatedTerm).withModifiers(translatedModifiersToApply)

      translatedExpr match {
        case translatedTerm: Term => Try(makeExprFrom(translatedTerm))
        case translatedOr: Or => {
          debug("Applying modifiers to multi-term translation result")

          Try(translatedOr.map {
            case t: Term => makeExprFrom(t)
            case e: Expression => e
          })
        }
        //NB: Intentionally fail loudly
        case unexpected => mappingFailure(s"Unexpected translation result: '$unexpected'")
      }
    }

    def applyModifiersToSubexpressions(translatedExpr: Expression, translatedModifierKeyExpr: Expression): Try[Expression] = {
      val translatedModifierKey = translatedModifierKeyExpr.asInstanceOf[Term].value
      val translatedModifiers = this.modifiers.map(_.copy(key = translatedModifierKey))

      constrainTranslatedExpression(translatedExpr, translatedModifiers)
    }

    modifiers match {
      case None => {
        for {
          translatedExpr <- term.translate(lookup)
          result <- constrainTranslatedExpression(translatedExpr, None)
        } yield result
      }
      case Some(mods) => {
        for {
          translatedExpr <- term.translate(lookup)
          translatedModifierKeyExpr <- translateModifierKey(mods.key)
          result <- applyModifiersToSubexpressions(translatedExpr, translatedModifierKeyExpr)
        } yield {
          result
        }
      }
    }
  }
}

object Constrained {
  def apply(term: Term, modifiers: Modifiers, valueConstraint: ValueConstraint) = new Constrained(term, Option(modifiers), Option(valueConstraint))
}

final case class WithTiming(timing: PanelTiming, expr: Expression) extends Expression with MappableExpression with HasSingleSubExpression with KnowsOwnType {
  override type MyType = WithTiming

  override def withExpr(newExpr: Expression) = if (newExpr eq expr) this else this.copy(expr = newExpr)

  override def toXml: NodeSeq = XmlUtil.stripWhitespace {
    <withTiming>
      <timing>{ timing }</timing>
      <expr>
        { expr.toXml }
      </expr>
    </withTiming>
  }

  override def toJson: JValue = {
    "withTiming" -> {
      ("timing" -> timing.name) ~
        ("expression" -> expr.toJson)
    }
  }

  override def normalize: Expression = {
    val normalizedExpr = expr.normalize

    timing match {
      case PanelTiming.Any => normalizedExpr
      case _ => withExpr(normalizedExpr)
    }
  }

  override def hasDirectI2b2Representation = expr.hasDirectI2b2Representation

  override def toExecutionPlan = SimplePlan(this.normalize)

  override def map(f: Expression => Expression): WithTiming = withExpr(f(expr))

  override def translate(lookup: String => Set[String]): Try[Expression] = expr.translate(lookup).map(withExpr)
}

final case class Not(expr: Expression) extends Expression with MappableExpression with HasSingleSubExpression with KnowsOwnType {
  type MyType = Not

  override def withExpr(newExpr: Expression) = if (newExpr eq expr) this else this.copy(expr = newExpr)

  override def toXml: NodeSeq = XmlUtil.stripWhitespace(<not>{ expr.toXml }</not>)

  override def toJson: JValue = ("not" -> expr.toJson)

  override def normalize = {
    expr match {
      //Collapse repeated Nots: Not(Not(e)) => e
      case Not(e) => e.normalize
      case _ => this.withExpr(expr.normalize)
    }
  }

  override def hasDirectI2b2Representation = expr.hasDirectI2b2Representation

  override def toExecutionPlan = SimplePlan(this.normalize)

  override def map(f: Expression => Expression): Not = withExpr(f(expr))

  override def translate(lookup: String => Set[String]): Try[Expression] = expr.translate(lookup).map(withExpr)
}

abstract class ComposeableExpression[T <: ComposeableExpression[T]: Manifest](Op: (Expression*) => T, override val exprs: Expression*) extends Expression with HasSubExpressions with MappableExpression with KnowsOwnType {
  override type MyType = T

  import ExpressionHelpers.is

  def containsA[E: Manifest] = exprs.exists(is[E])

  def ++(es: Iterable[Expression]): T = Op((exprs ++ es): _*)

  def merge(other: T): T = Op((exprs ++ other.exprs): _*)

  private[query] lazy val empty: T = Op()

  private[query] def toIterable(e: Expression): Iterable[Expression] = e match {
    case op: T if is[T](op) => op.exprs
    case _ => Seq(e)
  }

  override def map(f: Expression => Expression): T = Op(exprs.map(f): _*)

  def filter(p: Expression => Boolean): T = Op(exprs.filter(p): _*)

  override def normalize: Expression = {
    val result = exprs.map(_.normalize) match {
      case x if x.isEmpty => this
      case Seq(expr) => expr
      case es => es.foldLeft(empty)((accumulator, expr) => accumulator ++ toIterable(expr))
    }

    result match {
      case op: T if is[T](op) && op.containsA[T] => op.normalize
      case _ => result
    }
  }
}

final case class And(override val exprs: Expression*) extends ComposeableExpression[And](And, exprs: _*) {

  override def toString = "And(" + exprs.mkString(",") + ")"

  override def toXml: NodeSeq = XmlUtil.stripWhitespace(<and>{ exprs.map(_.toXml) }</and>)

  override def toJson: JValue = ("and" -> exprs.map(_.toJson))

  override def hasDirectI2b2Representation = exprs.forall(_.hasDirectI2b2Representation)

  override def toExecutionPlan: ExecutionPlan = {
    if (hasDirectI2b2Representation) {
      SimplePlan(this.normalize)
    } else {
      CompoundPlan.And(exprs.map(_.toExecutionPlan): _*).normalize
    }
  }

  override def translate(lookup: String => Set[String]): Try[Expression] = {
    if (exprs.isEmpty) { mappingFailure("Empty and-expressions are considered invalid") }
    else {
      val translatedSubExprAttempts: Seq[Try[Expression]] = exprs.map(_.translate(lookup))

      //All sub-expressions must be translatable for an And-expression to be translatable.
      //Util.Tries.sequence will return the first failure, if any
      for {
        translatedSubExprs <- Tries.sequence(translatedSubExprAttempts)
      } yield And(translatedSubExprs: _*)
    }
  }
}

final case class Or(override val exprs: Expression*) extends ComposeableExpression[Or](Or, exprs: _*) {

  override def toString = "Or(" + exprs.mkString(",") + ")"

  override def toXml: NodeSeq = XmlUtil.stripWhitespace(<or>{ exprs.map(_.toXml) }</or>)

  override def toJson: JValue = ("or" -> exprs.map(_.toJson))

  import ExpressionHelpers.is

  override def hasDirectI2b2Representation: Boolean = exprs.forall(e => !is[And](e) && e.hasDirectI2b2Representation)

  override def toExecutionPlan: ExecutionPlan = {
    if (hasDirectI2b2Representation) {
      SimplePlan(this.normalize)
    } else {
      val (ands, notAnds) = exprs.partition(is[And])

      val andPlans = ands.map(_.toExecutionPlan)

      val andCompound = CompoundPlan.Or(andPlans: _*)

      if (notAnds.isEmpty) {
        andCompound
      } else {
        val notAndPlans = notAnds.map(_.toExecutionPlan)

        val consolidatedNotAndPlan = notAndPlans.reduce(_ or _)

        val components: Seq[ExecutionPlan] = andPlans.size match {
          case 1 => andPlans :+ consolidatedNotAndPlan
          case _ => if (ands.isEmpty) Seq(consolidatedNotAndPlan) else Seq(andCompound, consolidatedNotAndPlan)
        }

        val result = components match {
          case Seq(plan: CompoundPlan) => plan
          case _ => CompoundPlan.Or(components: _*)
        }

        result.normalize
      }
    }
  }

  override def translate(lookup: String => Set[String]): Try[Expression] = {
    val translatedSubExprAttempts: Seq[Try[Expression]] = exprs.map(_.translate(lookup))

    val translatedSubExprs = translatedSubExprAttempts.collect { case Success(expr) => expr }

    if (translatedSubExprs.isEmpty) { mappingFailure(s"Or-expressions must have one valid (mappable) subexpression to be valid.  No valid (mappable) sub-expressions in '$this'") }
    else { Try(Or(translatedSubExprs: _*)) }
  }
}

final case class DateBounded(start: Option[XMLGregorianCalendar], end: Option[XMLGregorianCalendar], expr: Expression) extends Expression with MappableExpression with HasSingleSubExpression with KnowsOwnType {

  override type MyType = DateBounded

  override def withExpr(newExpr: Expression) = if (newExpr eq expr) this else this.copy(expr = newExpr)

  override def toXml: NodeSeq = XmlUtil.stripWhitespace {
    <dateBounded>
      { start.map(x => <start>{ x }</start>).getOrElse(<start/>) }
      { end.map(x => <end>{ x }</end>).getOrElse(<end/>) }
      { expr.toXml }
    </dateBounded>
  }

  override def toJson: JValue = ("dateBounded" ->
    ("start" -> start.map(_.toString)) ~
    ("end" -> end.map(_.toString)) ~
    ("expression" -> expr.toJson))

  override def normalize = {
    def normalize(date: Option[XMLGregorianCalendar]) = date.map(_.normalize)

    if (start.isEmpty && end.isEmpty) {
      expr.normalize
    } else {
      //NB: Dates are normalized to UTC.  I don't know if this is right, but it's what the existing webclient seems to do.
      val normalizedSubExpr = expr.normalize
      val normalizedStart = normalize(start)
      val normalizedEnd = normalize(end)

      DateBounded(normalizedStart, normalizedEnd, normalizedSubExpr)
    }
  }

  override def toExecutionPlan = SimplePlan(this.normalize)

  override def hasDirectI2b2Representation = expr.hasDirectI2b2Representation

  override def map(f: Expression => Expression): DateBounded = withExpr(f(expr))

  override def translate(lookup: String => Set[String]): Try[Expression] = expr.translate(lookup).map(withExpr)
}

final case class OccuranceLimited(min: Int, expr: Expression) extends Expression with MappableExpression with HasSingleSubExpression with KnowsOwnType {

  require(min >= 1)

  override type MyType = OccuranceLimited

  override def withExpr(newExpr: Expression) = if (newExpr eq expr) this else this.copy(expr = newExpr)

  override def toXml: NodeSeq = XmlUtil.stripWhitespace {
    <occurs>
      <min>{ min }</min>
      { expr.toXml }
    </occurs>
  }

  override def toJson: JValue = {
    "occurs" -> {
      ("min" -> min) ~
        ("expression" -> expr.toJson)
    }
  }

  override def normalize = if (min == 1) expr.normalize else this.withExpr(expr.normalize)

  override def toExecutionPlan = SimplePlan(this.normalize)

  override def hasDirectI2b2Representation = expr.hasDirectI2b2Representation

  override def map(f: Expression => Expression): OccuranceLimited = this.copy(expr = f(expr))

  override def translate(lookup: String => Set[String]): Try[Expression] = expr.translate(lookup).map(withExpr)
}