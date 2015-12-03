package net.shrine.protocol

import net.shrine.util.ShouldMatchersForJUnit
import org.junit.Test
import net.shrine.protocol.query.QueryDefinition
import net.shrine.protocol.query.Term

/**
 * @author clint
 * @date Feb 13, 2014
 */
final class NonI2b2ShrineResponseTest extends ShouldMatchersForJUnit {
  @Test
  def testFromXml {
    import NonI2b2ShrineResponse.fromXml
    
    fromXml(DefaultBreakdownResultOutputTypes.toSet)(<foo/>).isFailure should be(true)
    
    {
      val resp = SingleNodeReadTranslatedQueryDefinitionResponse(SingleNodeTranslationResult(NodeId("nuh"), QueryDefinition("foo", Term("blarg"))))
      
      fromXml(DefaultBreakdownResultOutputTypes.toSet)(resp.toXml).get should equal(resp)
    }
    
    {
      val resp = AggregatedReadTranslatedQueryDefinitionResponse(Seq(
          SingleNodeTranslationResult(NodeId("nuh"), QueryDefinition("foo", Term("blarg"))),
          SingleNodeTranslationResult(NodeId("zuh"), QueryDefinition("bar", Term("asdf")))
          ))
      
      fromXml(DefaultBreakdownResultOutputTypes.toSet)(resp.toXml).get should equal(resp)
    }
  }
}