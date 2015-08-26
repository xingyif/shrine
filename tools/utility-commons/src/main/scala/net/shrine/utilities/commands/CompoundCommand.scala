package net.shrine.utilities.commands

/**
 * @author clint
 * @date Mar 21, 2013
 */
final case class CompoundCommand[A, B, C](first: A >>> B, second: B >>> C) extends (A >>> C) {
  override def apply(a: A): C = second(first(a))
  
  override def toString = s"CompoundCommand($first, $second)"
}