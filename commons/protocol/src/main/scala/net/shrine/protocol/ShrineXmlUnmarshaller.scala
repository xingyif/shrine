package net.shrine.protocol

import scala.xml.NodeSeq
import scala.util.Try
import scala.xml.XML
import net.shrine.util.StringEnrichments

/**
 * @author clint
 * @date Oct 28, 2014
 */
trait ShrineXmlUnmarshaller[+T] {
  def fromXml(breakdownTypes: Set[ResultOutputType])(xml: NodeSeq): Try[T]

  import StringEnrichments._
  
  def fromXmlString(breakdownTypes: Set[ResultOutputType])(xmlString: String): Try[T] = {
    xmlString.tryToXml.flatMap(fromXml(breakdownTypes))
  }
}