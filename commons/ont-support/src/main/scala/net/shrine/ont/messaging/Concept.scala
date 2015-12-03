package net.shrine.ont.messaging

import net.liftweb.json._
import net.liftweb.json.JsonDSL._
import net.shrine.serialization.JsonMarshaller
import net.shrine.ont.OntTerm

/**
 * @author Justin Quan
 * @author Clint Gilbert
 * @date 9/1/11
 * @link http://chip.org
 *       <p/>
 *       NOTICE: This software comes with NO guarantees whatsoever and is
 *       licensed as Lgpl Open Source
 * @link http://www.gnu.org/licenses/lgpl.html
 */
final case class Concept(val path: String, val synonym: Option[String], val baseCode: Option[String] = None) extends JsonMarshaller {
  import OntTerm.{ forwardSlash, shrinePrefix }

  override def toJson: JValue = ("path" -> path) ~ ("synonym" -> synonym) ~ ("category" -> category) ~ ("simpleName" -> simpleName) ~ ("baseCode" -> baseCode)

  def category: String = {
    require(path.startsWith(shrinePrefix))

    val withoutShrinePrefix = path.drop(shrinePrefix.size)

    withoutShrinePrefix.takeWhile(_ != forwardSlash)
  }

  def simpleName: String = {
    val withoutTrailingSlash = if (path.last == forwardSlash) path.dropRight(1) else path

    val lastSlashIndex = withoutTrailingSlash.lastIndexWhere(_ == forwardSlash)

    withoutTrailingSlash.substring(lastSlashIndex + 1)
  }
}

object Concept extends LiftJsonUnmarshaller[Concept]