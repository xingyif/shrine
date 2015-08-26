package net.shrine.authorization

import net.shrine.util.ShouldMatchersForJUnit
import org.junit.Test

/**
 * @author clint
 * @date Dec 13, 2013
 */
final class AuthorizationResultTest extends ShouldMatchersForJUnit {
  @Test
  def testIsAuthorized {
    import AuthorizationResult._
    
    Authorized.isAuthorized should be(true)
    NotAuthorized("user foo can't do that").isAuthorized should be(false)
  }
}