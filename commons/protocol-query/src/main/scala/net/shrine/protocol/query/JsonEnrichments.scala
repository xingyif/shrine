package net.shrine.protocol.query

import net.liftweb.json.JsonAST._
import scala.util.Try
import scala.util.Success
import scala.util.Failure

/**
 * @author clint
 * @date Jul 24, 2014
 */
object JsonEnrichments {
  implicit final class HasWithChild(val json: JValue) extends AnyVal {
    def withChildString(childName: String): Try[String] = withChild(childName, JString.unapply(_))

    def withChild[J <: JValue : Manifest, T](childName: String, extractor: J => Try[T])(implicit discriminator: Int = 42): Try[T] = {
      withChild(childName, (j: J) => extractor(j).toOption)
    }

    def withChild[J <: JValue : Manifest, T](childName: String, extractor: J => Option[T]): Try[T] = {
      def isAJ(j: J): Boolean = manifest[J].runtimeClass.isAssignableFrom(j.getClass)
      
      json match {
        case null => Failure(new Exception("Null JSON value passed in"))
        case _ => json.children.collect {
          case JField(name, value: J) if isAJ(value) && name == childName => extractor(value)
        }.flatten.headOption.map(Success(_)).getOrElse {
          val children = json.children.collect { case JField(name, _) => s"'$name'" }.mkString(",")
          
          Failure(new Exception(s"Couldn't find child with name '$childName' in '$json'; children are $children"))
        }
      }
    }
  }
}