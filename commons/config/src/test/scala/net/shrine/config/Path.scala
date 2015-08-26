package net.shrine.config

import java.io.File

/**
 * @author clint
 * @date Oct 22, 2013
 */
object Path {
  def apply(parts: String*): String = parts.mkString(File.separator)
}