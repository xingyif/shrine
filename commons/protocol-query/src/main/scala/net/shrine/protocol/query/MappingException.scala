package net.shrine.protocol.query

/**
 * @author clint
 * @date Dec 19, 2014
 */
final class MappingException(message: String, cause: Throwable) extends RuntimeException(message, cause) {
  def this(message: String) = this(message, null)
  
  def this(cause: Throwable) = this("", cause)
  
  def this() = this("", null)
}