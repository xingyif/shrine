package net.shrine.utilities

/**
 * @author clint
 * @date Oct 17, 2013
 */
package object scallop {
  type Parser[A] = PartialFunction[(String, List[String]), Either[String, Option[A]]]
}