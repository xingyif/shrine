package net.shrine.client

/**
 * @author clint
 * @date Dec 16, 2013
 */
final class TimeoutException(message: String, cause: Throwable) extends RuntimeException(message, cause) {
  def this(message: String) = this(message, null)
  
  def this(cause: Throwable) = this("", cause)
}