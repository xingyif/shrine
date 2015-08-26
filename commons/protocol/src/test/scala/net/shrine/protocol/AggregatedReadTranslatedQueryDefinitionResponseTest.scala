package net.shrine.protocol

import net.shrine.util.ShouldMatchersForJUnit
import org.junit.Test
import net.shrine.util.XmlUtil
import net.shrine.protocol.query.QueryDefinition
import net.shrine.protocol.query.Term

/**
 * @author clint
 * @date Feb 13, 2014
 */
final class AggregatedReadTranslatedQueryDefinitionResponseTest extends AbstractTranslatedQueryDefinitionTest with ShouldMatchersForJUnit {
  private val nodeId1 = NodeId("somewhere")
  private val nodeId2 = NodeId("somewhere else")
  
  private val queryDef1 = queryDef
  private val queryDef2 = QueryDefinition("asjkhkjdfhksdf", Term("nuh"))
  
  private val xmlString = XmlUtil.stripWhitespace {
    <aggregatedReadTranslatedQueryDefinitionResponse>
      <translationResult>
          { nodeId1.toXml }
          { queryDef1.toXml }
        </translationResult>
        <translationResult>
          { nodeId2.toXml }
          { queryDef2.toXml }
        </translationResult>
	</aggregatedReadTranslatedQueryDefinitionResponse>
  }.toString
  
  private val result1 = SingleNodeTranslationResult(nodeId1, queryDef1)
  private val result2 = SingleNodeTranslationResult(nodeId2, queryDef2)
  
  private val resp = AggregatedReadTranslatedQueryDefinitionResponse(Seq(result1, result2))

  private val expectedEmptyXml = <aggregatedReadTranslatedQueryDefinitionResponse><noTranslationResults/></aggregatedReadTranslatedQueryDefinitionResponse>.toString
  
  import AggregatedReadTranslatedQueryDefinitionResponse.Empty
  
  @Test
  def testEmptyToXml {
    Empty.toXmlString should equal(expectedEmptyXml)
  }
  
  @Test
  def testToXml {
    resp.toXmlString should equal(xmlString) 
  }
  
  @Test
  def testEmptyFromXml {
    AggregatedReadTranslatedQueryDefinitionResponse.fromXml(expectedEmptyXml).get should equal(Empty)
  }
  
  @Test
  def testFromXml {
    AggregatedReadTranslatedQueryDefinitionResponse.fromXml(xmlString).get should equal(resp)
  }
  
  @Test
  def testEmptyRoundTrip {
    AggregatedReadTranslatedQueryDefinitionResponse.fromXml(Empty.toXml).get should equal(Empty)
  }
  
  @Test
  def testRoundTrip {
    AggregatedReadTranslatedQueryDefinitionResponse.fromXml(resp.toXml).get should equal(resp)
  }
}
