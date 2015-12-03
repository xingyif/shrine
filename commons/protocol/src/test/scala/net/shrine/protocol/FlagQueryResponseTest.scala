package net.shrine.protocol

import net.shrine.util.ShouldMatchersForJUnit
import org.junit.Test
import net.shrine.util.NodeSeqEnrichments

/**
 * @author clint
 * @date Jun 17, 2014
 */
final class FlagQueryResponseTest extends ShouldMatchersForJUnit {
  @Test
  def testShrineXmlRoundTrip: Unit = {
    val resp = FlagQueryResponse
      
    FlagQueryResponse.fromXml(resp.toXml).get should equal(resp)
  }
  
  @Test
  def testI2b2XmlRoundTrip: Unit = {
    val resp = FlagQueryResponse
      
    FlagQueryResponse.fromI2b2(resp.toI2b2).get should equal(resp)
  }
  
  @Test
  def testToXml: Unit = {
    //Compare XML as string's, because Scala XML comparison is sometimes wonky
    FlagQueryResponse.toXmlString should equal("<flagQueryResponse/>")
  }
  
  @Test
  def testToI2b2: Unit = {
    val xml = FlagQueryResponse.toI2b2 
    
    import NodeSeqEnrichments.Strictness._
    
    //Compare XML as string's, because Scala XML comparison is sometimes wonky
    (xml withChild "message_body" withChild "flagQueryResponse").get.toString should equal("<flagQueryResponse/>")
  }
}