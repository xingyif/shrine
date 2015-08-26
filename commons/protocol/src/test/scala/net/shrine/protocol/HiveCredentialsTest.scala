package net.shrine.protocol

import net.shrine.protocol.HiveCredentials
import net.shrine.util.ShouldMatchersForJUnit

/**
 * @author clint
 * @since Oct 4, 2012
 */
final class HiveCredentialsTest extends ShouldMatchersForJUnit {
  def testToAuthenticationInfo {
    val creds = HiveCredentials("domain", "username", "password", "project")
    
    val authn = creds.toAuthenticationInfo
    
    authn should not be(null)
    
    authn.domain should equal(creds.domain)
    authn.username should equal(creds.username)
    authn.credential.value should equal(creds.password)
    authn.credential.isToken should be(false)
  }
}