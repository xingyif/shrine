package net.shrine.authentication

/**
 * @author clint
 * @date Dec 13, 2013
 */
final class NotAuthenticatedException(message: String, cause: Throwable) extends RuntimeException(message, cause) {
  def this() = this(null, null)

  def this(message: String) = this(message, null)

  def this(e: Throwable) = this(null, e)
}
