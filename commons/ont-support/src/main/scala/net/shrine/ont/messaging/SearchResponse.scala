package net.shrine.ont.messaging
import net.liftweb.json._
import net.liftweb.json.JsonDSL._
import net.shrine.serialization.JsonMarshaller

/**
 * @author Dave Ortiz
 * @date 11/2/11
 * @link http://cbmi.med.harvard.edu
 * @link http://chip.org
 *       <p/>
 *       NOTICE: This software comes with NO guarantees whatsoever and is
 *       licensed as Lgpl Open Source
 * @link http://www.gnu.org/licenses/lgpl.html
 */
final case class SearchResponse(val originalQuery: String, val concepts: Seq[Concept]) extends JsonMarshaller {
  override def toJson = ("originalQuery" -> originalQuery) ~ ("concepts" -> concepts.map(_.toJson))
}

object SearchResponse extends LiftJsonUnmarshaller[SearchResponse]