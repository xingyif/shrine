package net.shrine.wiring

import net.shrine.authentication.{AuthenticationType, PmAuthenticator}
import net.shrine.authorization.{AllowsAllAuthorizationService, AuthorizationType}
import net.shrine.client.Poster
import net.shrine.hms.authentication.EcommonsPmAuthenticator
import net.shrine.qep.AllowsAllAuthenticator
import net.shrine.util.ShouldMatchersForJUnit
import org.junit.Test

/**
 * @author clint
 * @since Jul 2, 2014
 */
final class AuthStrategyTest extends ShouldMatchersForJUnit {
  private[this] val pmPoster = Poster("http://example.com", null)
  
  @Test
  def testDefaultDetermineAuthenticator(): Unit = {
    import AuthenticationType._
    
    intercept[Exception] {
      AuthStrategy.determineAuthenticator(null, pmPoster)
    }
    
    {
      val authenticator = AuthStrategy.determineAuthenticator(Pm, pmPoster).asInstanceOf[PmAuthenticator]
      
      authenticator.pmPoster should be(pmPoster)
    }
    
    {
      val authenticator = AuthStrategy.determineAuthenticator(Ecommons, pmPoster).asInstanceOf[EcommonsPmAuthenticator]
      
      authenticator.pmPoster should be(pmPoster)
    }
    
    {
      val authenticator = AuthStrategy.determineAuthenticator(NoAuthentication, pmPoster)
      
      authenticator should be(AllowsAllAuthenticator)
    }
  }
}