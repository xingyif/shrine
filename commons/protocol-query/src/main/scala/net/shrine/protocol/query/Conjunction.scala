package net.shrine.protocol.query

/**
 * @author clint
 * @date Dec 10, 2012
 */
sealed abstract class Conjunction(val combine: (Expression*) => Expression) {
  //def combine(exprs: Expression*) = combinator(exprs: _*)
}

object Conjunction {
  case object And extends Conjunction(net.shrine.protocol.query.And.apply)
  case object Or extends Conjunction(net.shrine.protocol.query.Or.apply)
}