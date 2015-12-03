package net.shrine.wiring

import javax.naming.InitialContext
import scala.util.Try

/**
 * @author clint
 * @date Jan 15, 2014
 */
object Jndi {
  def apply[T](name: String): Try[T] = Try {
    (new InitialContext).lookup(name).asInstanceOf[T]
  }
}