package net.shrine.util

import scala.util.Try
import scala.xml.NodeSeq
import scala.xml.XML

/**
 * @author clint
 * @date Nov 26, 2014
 */
object StringEnrichments {
  final implicit class HasStringEnrichments(val s: String) extends AnyVal {
    def tryToXml: Try[NodeSeq] = Try(XML.loadString(s))
  }
}