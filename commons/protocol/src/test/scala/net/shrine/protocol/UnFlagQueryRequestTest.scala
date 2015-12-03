package net.shrine.protocol

import net.shrine.util.ShouldMatchersForJUnit
import org.junit.Test
import scala.xml.NodeSeq
import scala.util.Try

/**
 * @author clint
 * @date Jun 17, 2014
 */
final class UnFlagQueryRequestTest extends ShouldMatchersForJUnit {
  @Test
  def testShrineXmlRoundTrip: Unit = {
    doSerializationRoundTrip(_.toXml, UnFlagQueryRequest.fromXml(DefaultBreakdownResultOutputTypes.toSet))

    UnFlagQueryRequest.fromXml(DefaultBreakdownResultOutputTypes.toSet)(<foo/>).isFailure should be(true)
  }

  @Test
  def testI2b2XmlRoundTrip: Unit = {
    doSerializationRoundTrip(_.toI2b2, UnFlagQueryRequest.fromI2b2(DefaultBreakdownResultOutputTypes.toSet))

    UnFlagQueryRequest.fromI2b2(DefaultBreakdownResultOutputTypes.toSet)(<foo/>).isFailure should be(true)
  }
  
  @Test
  def testUnmarshalViaHandleableI2b2Request: Unit = {
    doSerializationRoundTrip(_.toI2b2, HandleableI2b2Request.fromI2b2(DefaultBreakdownResultOutputTypes.toSet)(_).asInstanceOf[Try[UnFlagQueryRequest]])
  }

  private def doSerializationRoundTrip(serialize: UnFlagQueryRequest => NodeSeq, deserialize: NodeSeq => Try[UnFlagQueryRequest]): Unit = {
    import scala.concurrent.duration._

    val authn = AuthenticationInfo("some-domain", "some-user", Credential("laksjd", false))

    val req = UnFlagQueryRequest("some-project-id", 55.hours, authn, 12345L)

    val xml = serialize(req)

    val unmarshalled = deserialize(xml).get

    unmarshalled should equal(req)
  }
}