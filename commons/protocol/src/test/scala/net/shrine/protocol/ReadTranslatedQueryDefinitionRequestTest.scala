package net.shrine.protocol

import net.shrine.util.ShouldMatchersForJUnit
import org.junit.Test
import net.shrine.protocol.query.Or
import net.shrine.protocol.query.Term
import net.shrine.protocol.query.QueryDefinition
import net.shrine.util.DurationEnrichments
import net.shrine.util.XmlUtil

/**
 * @author clint
 * @date Feb 13, 2014
 */
final class ReadTranslatedQueryDefinitionRequestTest extends AbstractTranslatedQueryDefinitionTest with ShouldMatchersForJUnit {
  
  import scala.concurrent.duration._
  
  private val waitTime = 1.second
  
  import DurationEnrichments._
  
  private lazy val xmlString = <readTranslatedQueryDefinitionRequest>{ authn.toXml }{XmlUtil.renameRootTag("waitTime")(waitTime.toXml)}<queryDefinition><name>{ name }</name><expr><or><term>{ t1 }</term><term>{ t2 }</term></or></expr></queryDefinition></readTranslatedQueryDefinitionRequest>.toString
  
  private val authn = AuthenticationInfo("d", "u", Credential("p", false))
  
  private lazy val req = ReadTranslatedQueryDefinitionRequest(authn, waitTime, queryDef)
  
  @Test
  def testToXml {
    req.toXmlString should equal(xmlString)
  }
  
  @Test
  def testFromXml {
    ReadTranslatedQueryDefinitionRequest.fromXmlString(DefaultBreakdownResultOutputTypes.toSet)(xmlString).get should equal(req)
  }
  
  @Test
  def testRoundTrip {
    ReadTranslatedQueryDefinitionRequest.fromXml(DefaultBreakdownResultOutputTypes.toSet)(req.toXml).get should equal(req)
  }
}