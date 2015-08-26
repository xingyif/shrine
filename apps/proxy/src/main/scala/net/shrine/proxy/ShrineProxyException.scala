package net.shrine.proxy

/**
 * @author clint
 * @date May 1, 2013
 */
final class ShrineProxyException(message: String, cause: Throwable) extends Exception(message, cause) {
  def this() = this(null, null)

  def this(message: String) = this(message, null)

  def this(cause: Throwable) = this("", cause)
}