package net.shrine.utilities.scallop

import scala.reflect.runtime.universe._

/**
 * @author clint
 * @date Oct 17, 2013
 */
final class SimpleManifestValueConverter[A : TypeTag](parse: String => A) extends ManifestValueConverter[A] {
  override val parseFirst: Parser[A] = {
    case (_, Seq(param)) => Right {
      Option(parse(param))
    }
  }
}