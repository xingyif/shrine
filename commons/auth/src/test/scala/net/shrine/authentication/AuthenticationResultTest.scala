package net.shrine.authentication

import net.shrine.util.ShouldMatchersForJUnit
import org.junit.Test

/**
 * @author clint
 * @date Dec 13, 2013
 */
final class AuthenticationResultTest extends ShouldMatchersForJUnit {
  @Test
  def testIsAuthenticated {
    import AuthenticationResult._
    
    Authenticated("d", "u").isAuthenticated should be(true)
    
    NotAuthenticated("d", "u", "some reason").isAuthenticated should be(false)
  }
}