package net.shrine.authorization

import net.shrine.util.ShouldMatchersForJUnit
import org.junit.Test

/**
 * @author clint
 * @since Dec 13, 2013
 */
final class AuthorizationResultTest extends ShouldMatchersForJUnit {
  @Test
  def testIsAuthorized {
    import AuthorizationResult._
    
    Authorized(Option(("7","Some topic"))).isAuthorized should be(true)
    NotAuthorized("user foo can't do that").isAuthorized should be(false)
  }
}