package net.shrine.utilities.scallop

/**
 * @author clint
 * @date Oct 17, 2013
 */
object Keys {
  val minutes = "minutes"
  val seconds = "seconds"
  val milliseconds = "milliseconds"

  val domain = "domain"
  val username = "username"
  val password = "password"

  def subKey(base: String)(k: String) = s"$base.$k"
  
  def credentials(base: String): String = subKey(base)("credentials")
}