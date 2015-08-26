package net.shrine.serialization

import xml.{XML, NodeSeq}
import net.shrine.util.StringEnrichments
import scala.util.Try

/**
 * @author Bill Simons
 * @date 4/5/11
 * @link http://cbmi.med.harvard.edu
 * @link http://chip.org
 *       <p/>
 *       NOTICE: This software comes with NO guarantees whatsoever and is
 *       licensed as Lgpl Open Source
 * @link http://www.gnu.org/licenses/lgpl.html
 */
trait XmlUnmarshaller[+T] {
  def fromXml(xml: NodeSeq): T

  def fromXml(xmlString: String): T = fromXml(XML.loadString(xmlString))
  
  import StringEnrichments._
  
  def tryFromXml(xml: NodeSeq): Try[T] = Try(fromXml(xml))
  
  def tryFromXml(xmlString: String): Try[T] = xmlString.tryToXml.map(fromXml)
}