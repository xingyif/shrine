package net.shrine.protocol

import net.shrine.util.ShouldMatchersForJUnit
import org.junit.Test
import net.shrine.util.XmlUtil

/**
 * @author clint
 * @date Nov 2, 2012
 */
final class ReadQueryResultRequestTest extends ShouldMatchersForJUnit {

  private val authn = AuthenticationInfo("some-domain", "some-user", Credential("salkfa", false))

  import scala.concurrent.duration._
  
  private val req = ReadQueryResultRequest("some-project-id", 1.second, authn, 123)

  @Test
  def testToXml {
    val expected = XmlUtil.stripWhitespace {
      <readQueryResult>
        <projectId>some-project-id</projectId>
        <waitTimeMs>1000</waitTimeMs>
        { authn.toXml }
        <queryId>123</queryId>
      </readQueryResult>
    }.toString

    req.toXmlString should equal(expected)
  }

  @Test
  def testXmlRoundTrip {
    ReadQueryResultRequest.fromXml(DefaultBreakdownResultOutputTypes.toSet)(req.toXml).get should equal(req)
  }
}