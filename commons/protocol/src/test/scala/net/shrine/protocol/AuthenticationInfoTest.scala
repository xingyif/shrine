package net.shrine.protocol

import org.junit.Test
import org.junit.Assert.assertNotSame
import net.shrine.util.ShouldMatchersForJUnit

/**
 * @author Bill Simons
 * @date 3/11/11
 * @link http://cbmi.med.harvard.edu
 * @link http://chip.org
 *       <p/>
 *       NOTICE: This software comes with NO guarantees whatsoever and is
 *       licensed as Lgpl Open Source
 * @link http://www.gnu.org/licenses/lgpl.html
 */
final class AuthenticationInfoTest extends ShouldMatchersForJUnit with XmlSerializableValidator with I2b2SerializableValidator {
  @Test
  def testElidedToString: Unit = {
    AuthenticationInfo.elided.toString should equal("AuthenticationInfo(*******,*******,Credential(*******,false))")
  }
  
  private val auth = AuthenticationInfo("xyz-domain", "xyz-user", Credential("asjkdhkasjdfh", true))
  
  @Test
  def testToString: Unit = {
    auth.toString should equal(AuthenticationInfo.elided.toString)
  }
  
  @Test
  def testRawToString: Unit = {
    auth.rawToString should equal("AuthenticationInfo(xyz-domain,xyz-user,Credential(asjkdhkasjdfh,true))")
  }
  
  @Test
  def testStringConstructor {
    val copy = new AuthenticationInfo(auth.toHeader)

    assertNotSame(auth, copy)
    auth should equal(copy)
  }

  @Test
  def testFromI2b2 {
    val passwd = "SessionKey:prFsw9A1zZTr2PZpFLh1"
    val domain = "test_domain"
    val username = "test_username"
    val authn = AuthenticationInfo.fromI2b2 {
      <security>
        <domain>{ domain }</domain>
        <username>{ username }</username>
        <password token_ms_timeout="1800000" is_token="true">{ passwd }</password>
      </security>
    }.get

    authn.domain should equal(domain)
    authn.username should equal(username)
    authn.credential.value should equal(passwd)
    authn.credential.isToken should equal(true)
  }

  @Test
  def testToI2b2 {
    val credential = Credential("value", false)
    val domain = "domain"
    val username = "username"
    val authn = AuthenticationInfo(domain, username, credential)
    authn.toI2b2 should equal(<security><domain>{ domain }</domain><username>{ username }</username>{ credential.toI2b2 }</security>)
  }

  @Test
  def testToXml {
    val credential = Credential("value", false)
    val authn = AuthenticationInfo("domain", "username", credential)
    authn.toXml should equal(<authenticationInfo><domain>domain</domain><username>username</username>{ credential.toXml }</authenticationInfo>)
  }

  @Test
  def testToHeader {
    val passwd = "SessionKey:prFsw9A1zZTr2PZpFLh1"
    val domain = "test_domain"
    val username = "test_username"
    val credential = Credential(passwd, false)

    import AuthenticationInfo.{ headerDelimiter => delim }
    import AuthenticationInfo.{ headerPrefix => prefix }

    val headerString = prefix + domain + delim + username + delim + credential.value + delim + credential.isToken

    val authn = AuthenticationInfo(domain, username, credential)

    authn.toHeader should equal(headerString)

    val roundTripped = AuthenticationInfo.fromHeader(authn.toHeader)

    authn.domain should equal(roundTripped.domain)
    authn.username should equal(roundTripped.username)
    authn.credential should equal(roundTripped.credential)
  }

  @Test
  def testFromXml {
    val credential = Credential("value", false)
    val authn = AuthenticationInfo.fromXml(<authenticationInfo><domain>domain</domain><username>username</username>{ credential.toXml }</authenticationInfo>).get
    authn.domain should equal("domain")
    authn.username should equal("username")
    authn.credential should equal(credential)
  }

  @Test
  def testFromHeader {
    val passwd = "SessionKey:prFsw9A1zZTr2PZpFLh1"
    val domain = "test_domain"
    val username = "test_username"
    val credential = Credential(passwd, false)

    import AuthenticationInfo.{ headerDelimiter => delim }
    import AuthenticationInfo.{ headerPrefix => prefix }

    val headerString = prefix + domain + delim + username + delim + credential.value + delim + credential.isToken

    val authn = AuthenticationInfo.fromHeader(headerString)

    authn should not be null
    authn.domain should equal(domain)
    authn.username should equal(username)
    authn.credential should equal(credential)
  }
}