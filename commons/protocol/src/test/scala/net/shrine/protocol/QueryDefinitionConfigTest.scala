package net.shrine.protocol

import javax.xml.datatype.{DatatypeConstants, XMLGregorianCalendar}

import net.liftweb.json.parse
import net.shrine.protocol.query.{QueryDefinition, Term}
import net.shrine.util.{ShouldMatchersForJUnit, XmlDateHelper}
import org.junit.Test

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
final class QueryDefinitionConfigTest extends ShouldMatchersForJUnit {

  @Test
  def testParsePanel {
    val startDate = XmlDateHelper.now.toString
    val endDate = XmlDateHelper.now.toString
    val panelJson = String.format("""{"invert" : true,
      "minOccurrences" : 0,
      "start" : "%s",
      "end" : "%s",
      "terms" : [
        {"value" : "term1"},
        {"value" : "term2"}
      ]}""", startDate, endDate)
    val actual = QueryDefinitionConfig.parsePanel(1, parse(panelJson))
    actual should not be (null)
    actual.start.get.toString should equal(startDate)
    actual.end.get.toString should equal(endDate)
    actual.inverted should be(true)
    actual.minOccurrences should equal(0)
    actual.terms.size should equal(2)
    actual.terms.contains(Term("term1")) should be(true)
    actual.terms.contains(Term("term2")) should be(true)
  }

  @Test
  def testParseQueryDefinition {
    val startCal = XmlDateHelper.now
    val endCal = XmlDateHelper.now
    val startDate = startCal.toString
    val endDate = endCal.toString
    val queryDefJson = String.format("""{"name": "query definition",
    "panels" : [
      {
        "start" : "%s",
        "end" : "%s",
        "terms" : [{"value" : "term1"}]
      },
      {
        "invert" : true,
        "minOccurrences" : 2,
        "terms" : [{"value" : "term2"}]
      }
    ]}""", startDate, endDate)
    val actual = QueryDefinitionConfig.parseQueryDefinition(parse(queryDefJson))
    val actualExpr = actual.expr.get
    val panels = QueryDefinition.toPanels(actualExpr)

    panels.size should equal(2)
    panels(0).number should equal(1)
    panels(0).inverted should be(false)
    panels(0).minOccurrences should equal(1)

    panels(0).start.get.toString should equal(startDate)
    panels(0).end.get.toString should equal(endDate)
    panels(0).terms.contains(Term("term1")) should be(true)

    panels(1).number should equal(2)
    panels(1).inverted should be(true)
    panels(1).start should equal(None)
    panels(1).end should equal(None)
    panels(1).terms.contains(Term("term2")) should be(true)
    panels(1).minOccurrences should equal(2)
  }

  @Test
  def testParseQueryDefinitionList {
    val queryDefConfig= """{"queryDefinitions": [
      {
        "name": "query definition one",
        "panels" : [{"terms" : [{"value" : "term1"}]}]
      }
      {
        "name": "query definition two",
        "panels" : [{"terms" : [{"value" : "term2"}]}]
      }
    ]}"""
    val config = QueryDefinitionConfig.parseQueryDefinitionConfig(queryDefConfig)
    config.size should equal (2)
    config(0).name should equal ("query definition one")
    config(1).name should equal ("query definition two")
  }
}