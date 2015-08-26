package net.shrine.authorization

import net.shrine.util.ShouldMatchersForJUnit
import org.junit.Test

/**
 * @author clint
 * @date Dec 13, 2013
 */
final class AllowsAllAuthorizationServiceTest extends ShouldMatchersForJUnit {
  @Test
  def testAuthorizeRunQueryRequest {
    AllowsAllAuthorizationService.authorizeRunQueryRequest(null) should be(AuthorizationResult.Authorized)
  }
  
  @Test
  def testReadApprovedEntries {
    intercept[UnsupportedOperationException] {
      AllowsAllAuthorizationService.readApprovedEntries(null)
    }
  }
}