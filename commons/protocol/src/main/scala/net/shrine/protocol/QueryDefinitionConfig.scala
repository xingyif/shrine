package net.shrine.protocol

import javax.xml.datatype.XMLGregorianCalendar

import net.liftweb.json._
import net.shrine.protocol.query.{And, Panel, PanelTiming, QueryDefinition, Term}
import net.shrine.util.XmlDateHelper

/**
 * @author Bill Simons
 * @since 3/28/12
 * @see http://cbmi.med.harvard.edu
 * @see http://chip.org
 *       <p/>
 *       NOTICE: This software comes with NO guarantees whatsoever and is
 *       licensed as Lgpl Open Source
 * @see http://www.gnu.org/licenses/lgpl.html
 */

//part of Heartbeat.groovy .
object QueryDefinitionConfig {

  def parseQueryDefinitionConfig(s: String): Seq[QueryDefinition] = {
    val json = parse(s)

    (json \ "queryDefinitions").children.map(parseQueryDefinition)
  }

  def parseQueryDefinition(json: JValue): QueryDefinition = {
    implicit val formats = DefaultFormats

    val name = (json \ "name").extract[String]
    val panels = (json \ "panels").children.zipWithIndex.map {
      case (panel: JValue, index: Int) => parsePanel(index + 1, panel)
    }

    val exprs = panels.map(_.toExpression)
    val consolidatedExpr = if (exprs.size == 1) exprs.head else And(exprs: _*)

    QueryDefinition(name, consolidatedExpr.normalize)
  }

  def parsePanel(panelNumber: Int, json: JValue): Panel = {
    implicit val formats = DefaultFormats

    def parseDate(stringRep: String): Option[XMLGregorianCalendar] = XmlDateHelper.parseXmlTime(stringRep).toOption
    
    val inverted = (json \ "invert").extractOpt[Boolean].getOrElse(false)
    val min = (json \ "minOccurrences").extractOpt[Int].getOrElse(1)
    val start = (json \ "start").extractOpt[String].flatMap(parseDate)
    val end = (json \ "end").extractOpt[String].flatMap(parseDate)
    val terms = (json \ "terms").extract[List[Term]]

    Panel(panelNumber, inverted, min, start, end, PanelTiming.Any, terms)
  }

  def loadQueryDefinitionConfig(fileName: String): java.util.Iterator[QueryDefinition] = {
    val source = scala.io.Source.fromFile(fileName)

    try {
      val lines = source.mkString

      import scala.collection.JavaConverters._

      parseQueryDefinitionConfig(lines).toIterator.asJava
    } finally {
      source.close()
    }
  }
}