package net.shrine.hms.authentication

import org.junit.Test
import net.shrine.util.ShouldMatchersForJUnit
import net.shrine.authentication.AuthenticationResult.Authenticated
import net.shrine.authentication.AuthenticationResult.NotAuthenticated
import net.shrine.client.HttpClient
import net.shrine.client.HttpResponse
import net.shrine.util.XmlUtil
import net.shrine.authentication.AuthenticationResult
import net.shrine.client.Poster
import net.shrine.protocol.AuthenticationInfo
import net.shrine.protocol.Credential

/**
 * @author clint
 * @date Dec 13, 2013
 */
final class EcommonsPmAuthenticatorTest extends ShouldMatchersForJUnit {
  
  import PmAuthenticatorTest._
  
  import AuthenticationResult._
  
  @Test
  def testAuthenticate {
    def authn(d: String, u: String, p: String) = AuthenticationInfo(d, u, Credential(p, false)) 
    
    //Calling Sheriff fails
    {
      val authenticator = new EcommonsPmAuthenticator(Poster("", AlwaysThrowsMockHttpClient))
      
      val result = authenticator.authenticate(authn(domain, username, ""))
      
      val NotAuthenticated(d, u, reason) = result
      
      d should equal(domain)
      u should equal(username)
      reason.isEmpty should be(false)
    }
    
    //Auth succeeds
    {
      val authenticator = new EcommonsPmAuthenticator(Poster("", AlwaysAuthenticatesMockHttpClient))
      
      val result = authenticator.authenticate(authn(domain, username, ""))
      
      val Authenticated(d, u) = result
      
      d should equal(domain)
      u should equal(ecommonsUsername)
    }
    
    //Auth fails
    {
      val authenticator = new EcommonsPmAuthenticator(Poster("", NeverAuthenticatesMockHttpClient))
      
      val result = authenticator.authenticate(authn(domain, username, ""))
      
      val NotAuthenticated(d, u, reason) = result
      
      d should equal(domain)
      u should equal(username)
      reason.isEmpty should be(false)
    }
  }
}

object PmAuthenticatorTest {
  object AlwaysAuthenticatesMockHttpClient extends HttpClient {
    override def post(input: String, url: String): HttpResponse = HttpResponse.ok(authenticatedResponse.toString)
  }

  object NeverAuthenticatesMockHttpClient extends HttpClient {
    override def post(input: String, url: String): HttpResponse = HttpResponse.ok(notAuthenticatedResponse.toString)
  }
  
  object AlwaysThrowsMockHttpClient extends HttpClient {
    override def post(input: String, url: String): HttpResponse = throw new Exception("blarg")
  }
  
  val domain = "d"
  val username = "u"
  val ecommonsUsername = "abc123"

  //NB: Response is intentionally stripped-down, with just the minimum needed to get past User.fromI2b2; this
  //avoids cluttering up this test with endless i2b2 verbosity
  private lazy val notAuthenticatedResponse = XmlUtil.stripWhitespace {
    <ns4:response xmlns:ns2="http://www.i2b2.org/xsd/hive/pdo/1.1/" xmlns:ns3="http://www.i2b2.org/xsd/cell/crc/pdo/1.1/" xmlns:ns4="http://www.i2b2.org/xsd/hive/msg/1.1/" xmlns:ns5="http://www.i2b2.org/xsd/cell/crc/psm/1.1/" xmlns:ns6="http://www.i2b2.org/xsd/cell/pm/1.1/" xmlns:ns7="http://sheriff.shrine.net/" xmlns:ns8="http://www.i2b2.org/xsd/cell/crc/psm/querydefinition/1.1/" xmlns:ns9="http://www.i2b2.org/xsd/cell/crc/psm/analysisdefinition/1.1/" xmlns:ns10="http://www.i2b2.org/xsd/cell/ont/1.1/" xmlns:ns11="http://www.i2b2.org/xsd/hive/msg/result/1.1/">
      <message_body>
        <ns6:configure>
          <user>
	  		<full_name>John Doe</full_name>
            <user_name>jd</user_name>
            <password token_ms_timeout="1800000" is_token="true">SessionKey:key</password>
            <domain>{ domain }</domain>
            <param name="foo">bar</param>
            <param name="nuh">zuh</param>
            <project id="fooProject">
              <name>Demo Group fooProject</name>
              <wiki>http://www.i2b2.org</wiki>
              <role>glarg</role>
            </project>
          </user>
        </ns6:configure>
      </message_body>
    </ns4:response>
  }
  
  //NB: Response is intentionally stripped-down, with just the minimum needed to get past User.fromI2b2; this
  //avoids cluttering up this test with endless i2b2 verbosity
  private lazy val authenticatedResponse = XmlUtil.stripWhitespace {
    <ns4:response xmlns:ns2="http://www.i2b2.org/xsd/hive/pdo/1.1/" xmlns:ns3="http://www.i2b2.org/xsd/cell/crc/pdo/1.1/" xmlns:ns4="http://www.i2b2.org/xsd/hive/msg/1.1/" xmlns:ns5="http://www.i2b2.org/xsd/cell/crc/psm/1.1/" xmlns:ns6="http://www.i2b2.org/xsd/cell/pm/1.1/" xmlns:ns7="http://sheriff.shrine.net/" xmlns:ns8="http://www.i2b2.org/xsd/cell/crc/psm/querydefinition/1.1/" xmlns:ns9="http://www.i2b2.org/xsd/cell/crc/psm/analysisdefinition/1.1/" xmlns:ns10="http://www.i2b2.org/xsd/cell/ont/1.1/" xmlns:ns11="http://www.i2b2.org/xsd/hive/msg/result/1.1/">
      <message_body>
        <ns6:configure>
          <user>
	  		<full_name>John Doe</full_name>
            <user_name>jd</user_name>
            <password token_ms_timeout="1800000" is_token="true">SessionKey:key</password>
            <domain>{ domain }</domain>s
            <param name="foo">bar</param>
            <param name="nuh">zuh</param>
	  		<param name="ecommons_username">{ ecommonsUsername }</param>
            <project id="fooProject">
              <name>Demo Group fooProject</name>
              <wiki>http://www.i2b2.org</wiki>
              <role>glarg</role>
            </project>
          </user>
        </ns6:configure>
      </message_body>
    </ns4:response>
  }
} 