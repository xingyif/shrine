package net.shrine.protocol

import net.shrine.util.ShouldMatchersForJUnit
import org.junit.Test
import net.shrine.util.NodeSeqEnrichments

/**
 * @author clint
 * @date Jun 17, 2014
 */
final class UnFlagQueryResponseTest extends ShouldMatchersForJUnit {
  @Test
  def testShrineXmlRoundTrip: Unit = {
    val resp = UnFlagQueryResponse
      
    UnFlagQueryResponse.fromXml(resp.toXml).get should equal(resp)
  }
  
  @Test
  def testI2b2XmlRoundTrip: Unit = {
    val resp = UnFlagQueryResponse
      
    UnFlagQueryResponse.fromI2b2(resp.toI2b2).get should equal(resp)
  }
  
  @Test
  def testToXml: Unit = {
    //Compare XML as string's, because Scala XML comparison is sometimes wonky
    UnFlagQueryResponse.toXmlString should equal("<unFlagQueryResponse/>")
  }
  
  @Test
  def testToI2b2: Unit = {
    val xml = UnFlagQueryResponse.toI2b2 
    
    import NodeSeqEnrichments.Strictness._
    
    //Compare XML as string's, because Scala XML comparison is sometimes wonky
    (xml withChild "message_body" withChild "unFlagQueryResponse").get.toString should equal("<unFlagQueryResponse/>")
  }
}