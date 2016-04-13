package net.shrine.wiring

import net.shrine.protocol.CredentialConfig
import net.shrine.util.ShouldMatchersForJUnit
import org.junit.Test
import net.shrine.client.{EndpointConfig, Poster}
import net.shrine.authentication.{AuthenticationType, PmAuthenticator, Authenticator}
import net.shrine.hms.authentication.EcommonsPmAuthenticator
import net.shrine.qep.{QepConfig, AllowsAllAuthenticator}
import net.shrine.authorization.{AuthorizationType, AllowsAllAuthorizationService}
import net.shrine.hms.authorization.HmsDataStewardAuthorizationService
import java.net.URL
import net.shrine.hms.authorization.JerseySheriffClient
import net.shrine.crypto.SigningCertStrategy

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
  
  @Test
  def testDefaultDetermineAuthorizationService(): Unit = {
    import AuthorizationType._
    
    intercept[Exception] {
      AuthStrategy.determineQueryAuthorizationService(null, null, null)
    }
    
    {
      val authService = AuthStrategy.determineQueryAuthorizationService(NoAuthorization, null, null)
      
      authService should be(AllowsAllAuthorizationService)
    }
    
    {
      val authenticationType = AuthenticationType.Ecommons
      val authorizationType = HmsSteward

      import scala.concurrent.duration._
      
      val sheriffUrl = "http://example.com/sheriff"
      val sheriffCredentials = CredentialConfig(None, "u", "p")
      
      val shrineConfig = ShrineConfig(
                          None, //hub config
                          Some(QepConfig(
                                authenticationType,
                                authorizationType,
                                Some(EndpointConfig(new URL(sheriffUrl), acceptAllCerts = false, 42.minutes)),
                                Some(sheriffCredentials),
                                None,
                                includeAggregateResults = false,
                                1.minute,
                                None,
                                SigningCertStrategy.Attach,
                                collectQepAudit = false)),
                          null //adapterStatusQuery
                        ) //breakdown types
      
      val authenticator: Authenticator = AllowsAllAuthenticator
      
      val authService = AuthStrategy.determineQueryAuthorizationService(HmsSteward, shrineConfig, authenticator).asInstanceOf[HmsDataStewardAuthorizationService]
      
      authService.authenticator should be(authenticator)
      //noinspection ScalaUnnecessaryParentheses
      authService.sheriffClient should not be(null)
      
      val jerseySheriffClient = authService.sheriffClient.asInstanceOf[JerseySheriffClient]
      
      jerseySheriffClient.sheriffUrl should equal(sheriffUrl)
      jerseySheriffClient.sheriffUsername should equal(sheriffCredentials.username)
      jerseySheriffClient.sheriffPassword should equal(sheriffCredentials.password)
    }
  }
}