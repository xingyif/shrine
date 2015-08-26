package net.shrine.serialization

import net.liftweb.json.JsonAST.JValue
import net.liftweb.json._
import text.Document

/**
 * @author Bill Simons
 * @date 3/20/12
 * @link http://cbmi.med.harvard.edu
 * @link http://chip.org
 *       <p/>
 *       NOTICE: This software comes with NO guarantees whatsoever and is
 *       licensed as Lgpl Open Source
 * @link http://www.gnu.org/licenses/lgpl.html
 */
trait JsonMarshaller {
  
  def toJson: JValue
  
  def toJsonString(style: (Document) => String = JsonMarshaller.COMPACT): String = style(render(toJson))
}

object JsonMarshaller {
  val COMPACT: Document => String = compact
  val PRETTY: Document => String = pretty
}