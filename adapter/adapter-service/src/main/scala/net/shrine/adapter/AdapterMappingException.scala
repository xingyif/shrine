package net.shrine.adapter

/**
 * @author Andrew McMurry
 * @author clint
 * @since ???
 * @since Nov 21, 2012 (Scala port)
 */
final class AdapterMappingException(message: String, cause: Throwable) extends AdapterException(message, cause) {
  def this() = this("", null)

  def this(message: String) = this(message, null)

  def this(cause: Throwable) = this("", cause)
}
