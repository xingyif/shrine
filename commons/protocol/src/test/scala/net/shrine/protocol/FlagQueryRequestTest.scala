package net.shrine.protocol

import net.shrine.util.ShouldMatchersForJUnit
import org.junit.Test
import scala.xml.NodeSeq
import scala.util.Try

/**
 * @author clint
 * @date Jun 17, 2014
 */
final class FlagQueryRequestTest extends ShouldMatchersForJUnit {
  @Test
  def testShrineXmlRoundTrip: Unit = {
    doSerializationRoundTrip(_.toXml, FlagQueryRequest.fromXml(DefaultBreakdownResultOutputTypes.toSet))
    
    FlagQueryRequest.fromXml(DefaultBreakdownResultOutputTypes.toSet)(<foo/>).isFailure should be(true)
  }
  
  @Test
  def testI2b2XmlRoundTrip: Unit = {
    doSerializationRoundTrip(_.toI2b2, FlagQueryRequest.fromI2b2(DefaultBreakdownResultOutputTypes.toSet))
    
    FlagQueryRequest.fromI2b2(DefaultBreakdownResultOutputTypes.toSet)(<foo/>).isFailure should be(true)
  }
  
  @Test
  def testUnmarshalViaHandleableI2b2Request: Unit = {
    doSerializationRoundTrip(_.toI2b2, HandleableI2b2Request.fromI2b2(DefaultBreakdownResultOutputTypes.toSet)(_).asInstanceOf[Try[FlagQueryRequest]])
  }
  
  private def doSerializationRoundTrip(serialize: FlagQueryRequest => NodeSeq, deserialize: NodeSeq => Try[FlagQueryRequest]): Unit = {
    import scala.concurrent.duration._

    val authn = AuthenticationInfo("some-domain", "some-user", Credential("laksjd", false))
    
    val noMessage = FlagQueryRequest("some-project-id", 55.hours, authn, 12345L, None)
    
    val withMessage = noMessage.copy(message = Some("flag message"))
    
    def doRoundTrip(req: FlagQueryRequest): Unit = {
      val xml = serialize(req)
      
      val unmarshalled = deserialize(xml).get
      
      unmarshalled should equal(req)
    }
    
    doRoundTrip(noMessage)
    doRoundTrip(withMessage)
  }
}