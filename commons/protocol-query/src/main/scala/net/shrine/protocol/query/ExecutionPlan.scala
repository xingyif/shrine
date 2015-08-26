package net.shrine.protocol.query

import net.shrine.util.XmlDateHelper

/**
 * @author clint
 * @date Nov 29, 2012
 */
sealed trait ExecutionPlan {
  def or(other: ExecutionPlan): ExecutionPlan

  def and(other: ExecutionPlan): ExecutionPlan

  def combine(conjunction: Conjunction)(other: ExecutionPlan): ExecutionPlan

  def normalize: ExecutionPlan

  def isSimple: Boolean

  final def isCompound: Boolean = !isSimple
}

/**
 * @author clint
 * @date Nov 29, 2012
 */
final case class SimplePlan(expr: Expression) extends ExecutionPlan {
  override def or(other: ExecutionPlan) = combine(Conjunction.Or)(other)

  override def and(other: ExecutionPlan) = combine(Conjunction.And)(other)

  override def combine(conjunction: Conjunction)(other: ExecutionPlan): ExecutionPlan = other match {
    case SimplePlan(otherExpr) => SimplePlan(conjunction.combine(expr, otherExpr).normalize)
    case _: CompoundPlan => CompoundPlan(conjunction, this, other)
  }

  def withExpr(e: Expression): SimplePlan = {
    if(e == expr) this
    else SimplePlan(e)
  }
  
  override def normalize: ExecutionPlan = withExpr(expr.normalize)

  override def isSimple: Boolean = true
}

/**
 * @author clint
 * @date Nov 29, 2012
 */
final case class CompoundPlan(conjunction: Conjunction, components: ExecutionPlan*) extends ExecutionPlan {
  override def toString = "CompoundPlan." + conjunction + "(" + components.mkString(",") + ")"

  override def or(other: ExecutionPlan) = CompoundPlan.Or(this, other)

  override def and(other: ExecutionPlan) = CompoundPlan.And(this, other)

  override def combine(conj: Conjunction)(other: ExecutionPlan) = conj match {
    case Conjunction.Or => or(other)
    case Conjunction.And => and(other)
  }

  override def isSimple: Boolean = false

  private[query] def isSame(conj: Conjunction) = conj == this.conjunction

  override def normalize: ExecutionPlan = {
    import CompoundPlan.Helpers._

    components match {
      case Seq(singlePlan) => singlePlan.normalize
      case _ => CompoundPlan(conjunction, components.flatMap {
        case CompoundPlan(conj, comps @ _*) if allSimple(comps) && isSame(conj) => {

          val asSimplePlans = comps.collect { case p: SimplePlan => p }

          val simpleQueriesGroupedByExprType: Map[Class[_], Seq[SimplePlan]] = asSimplePlans.groupBy(_.expr.getClass)
          
          val andPlans = simpleQueriesGroupedByExprType.get(classOf[And]).getOrElse(Seq.empty)

          val orPlans = simpleQueriesGroupedByExprType.get(classOf[Or]).getOrElse(Seq.empty)
          
          val otherPlans = (simpleQueriesGroupedByExprType - classOf[And] - classOf[Or]).values.flatten.toSeq

          val otherExprs = otherPlans.map(_.expr)
          
          val (orExprs: Seq[Or], andExprs: Seq[And]) = conj match {
            case Conjunction.Or => {
              val consolidatedOrExpr = flatten(ors(orPlans))
              
              (Seq(consolidatedOrExpr ++ otherExprs), ands(andPlans))
            }
            case Conjunction.And => {
              val consolidatedAndExpr = flatten(ands(andPlans))
              
              (ors(orPlans), Seq(consolidatedAndExpr ++ otherExprs))
            }
          }

          toPlans(orExprs) ++ toPlans(andExprs) ++ otherPlans
        }
        case c => Seq(c)
      }: _*)
    }
  }
}

object CompoundPlan {
  def Or(components: ExecutionPlan*): CompoundPlan = CompoundPlan(Conjunction.Or, components: _*)

  def And(components: ExecutionPlan*): CompoundPlan = CompoundPlan(Conjunction.And, components: _*)

  private[query] object Helpers {
    def allSimple(plans: Seq[ExecutionPlan]): Boolean = plans.forall(_.isSimple)

    def ands(plans: Seq[SimplePlan]): Seq[And] = plans.collect { case SimplePlan(a: And) => a }

    def ors(plans: Seq[SimplePlan]): Seq[Or] = plans.collect { case SimplePlan(o: Or) => o }

    def neitherAndsNorOrs(plans: Seq[SimplePlan]): Seq[SimplePlan] = {
      import ExpressionHelpers.is

      def isNeitherAndNorOr(expr: Expression) = !is[And](expr) && !is[Or](expr)

      plans.collect { case plan @ SimplePlan(expr) if isNeitherAndNorOr(expr) => plan }
    }

    private[query] trait HasZero[T] {
      def zero: T
    }

    private[query] object HasZero {
      import net.shrine.protocol.query.{ And => AndExpr }
      import net.shrine.protocol.query.{ Or => OrExpr }

      implicit val andHasZero: HasZero[AndExpr] = new HasZero[AndExpr] {
        override def zero = AndExpr()
      }

      implicit val orHasZero: HasZero[OrExpr] = new HasZero[OrExpr] {
        override def zero = OrExpr()
      }
    }

    def flatten[T <: ComposeableExpression[T] : HasZero](exprs: Seq[T]): T = {
      exprs.foldLeft(implicitly[HasZero[T]].zero)(_ merge _)
    }

    def toPlans(es: Seq[Expression]) = es.map(_.toExecutionPlan)
  }
}
