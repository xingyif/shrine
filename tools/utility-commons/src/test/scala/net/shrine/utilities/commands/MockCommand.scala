package net.shrine.utilities.commands

/**
 * @author clint
 * @date Mar 25, 2013
 */
final case class MockCommand[A, B](f: A => B) extends (A >>> B) {
  var invoked = false

  override def apply(a: A): B = {
    invoked = true

    f(a)
  }
}