package net.shrine.proxy;

/**
 * @author Andrew McMurry
 * @author Clint Gilbert
 * @date ???
 */
final class ShrineMessageFormatException(message: String, cause: Throwable) extends Exception(message, cause) {
  def this() = this(null, null)

  def this(message: String) = this(message, null)

  def this(cause: Throwable) = this("", cause)
}

