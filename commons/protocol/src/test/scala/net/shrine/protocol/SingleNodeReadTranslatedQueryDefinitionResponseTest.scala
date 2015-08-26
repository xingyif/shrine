package net.shrine.protocol

import net.shrine.util.ShouldMatchersForJUnit
import org.junit.Test
import net.shrine.util.XmlUtil

/**
 * @author clint
 * @date Feb 13, 2014
 */
final class SingleNodeReadTranslatedQueryDefinitionResponseTest extends AbstractTranslatedQueryDefinitionTest with ShouldMatchersForJUnit {
  
  private val nodeId = NodeId("somewhere")
  
  private val xmlString = XmlUtil.stripWhitespace {
    <singleNodeReadTranslatedQueryDefinitionResponse>
      <translationResult>
          { nodeId.toXml }
          { queryDef.toXml }
        </translationResult>
	</singleNodeReadTranslatedQueryDefinitionResponse>
  }.toString
  
  private val result = SingleNodeTranslationResult(nodeId, queryDef)
  
  private val resp = SingleNodeReadTranslatedQueryDefinitionResponse(result)
  
  @Test
  def testToXml {
    resp.toXmlString should equal(xmlString) 
  }
  
  @Test
  def testFromXml {
    SingleNodeReadTranslatedQueryDefinitionResponse.fromXml(xmlString).get should equal(resp)
  }
  
  @Test
  def testRoundTrip {
    SingleNodeReadTranslatedQueryDefinitionResponse.fromXml(resp.toXml).get should equal(resp)
  }
}