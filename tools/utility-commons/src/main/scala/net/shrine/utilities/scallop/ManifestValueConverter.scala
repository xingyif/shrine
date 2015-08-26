package net.shrine.utilities.scallop

import scala.reflect.runtime.universe._
import org.rogach.scallop.ValueConverter
import org.rogach.scallop.ArgType

/**
 * @author clint
 * @date Mar 26, 2013
 */
abstract class ManifestValueConverter[A : TypeTag] extends ValueConverter[A] {
    val parseFirst: Parser[A]

    override def parse(s: List[(String, List[String])]): Either[String, Option[A]] = {
      s.headOption.collect(parseFirst).getOrElse(Left(""))
    }

    override val tag: TypeTag[A] = typeTag[A]

    /** Type of parsed argument list. */
    override val argType: ArgType.V = ArgType.LIST
  }