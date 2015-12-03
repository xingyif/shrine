package net.shrine.utilities.mapping.sql

/**
 * @author clint
 * @date Jul 16, 2014
 */
sealed trait InputLocation

object InputLocation {
  case object FileSystem extends InputLocation
  case object ClassPath extends InputLocation
}