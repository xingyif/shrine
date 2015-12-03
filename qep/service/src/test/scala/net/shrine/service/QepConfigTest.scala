package net.shrine.service

import com.typesafe.config.ConfigFactory
import net.shrine.authentication.AuthenticationType
import net.shrine.authorization.AuthorizationType
import net.shrine.config.Keys
import net.shrine.util.ShouldMatchersForJUnit
import org.junit.Test

/**
 * @author clint
 * @since Mar 3, 2014
 */
final class QepConfigTest extends ShouldMatchersForJUnit {

  private def entryPointServiceConfig(baseFileName: String) = QepConfig(ConfigFactory.load(baseFileName).getConfig(s"shrine.${Keys.queryEntryPoint}"))
  
  @Test
  def testApply {
    val conf: QepConfig = entryPointServiceConfig("shrine")

    import scala.concurrent.duration._
    
    conf.authenticationType should be(AuthenticationType.Ecommons)
    
    conf.authorizationType should be(AuthorizationType.HmsSteward)
    
    conf.broadcasterIsLocal should be(false)
    
    conf.broadcasterServiceEndpoint.get.acceptAllCerts should be(true)
    conf.broadcasterServiceEndpoint.get.timeout should be(1.second)
    conf.broadcasterServiceEndpoint.get.url.toString should equal("http://example.com/shrine/rest/broadcaster/broadcast")
    
    conf.includeAggregateResults should equal(false)

    conf.maxQueryWaitTime should equal(5.minutes)
    
    conf.sheriffCredentials.get.domain should be(None)
    conf.sheriffCredentials.get.username should be("sheriffUsername")
    conf.sheriffCredentials.get.password should be("sheriffPassword")
    
    conf.sheriffEndpoint.get.acceptAllCerts should be(true)
    conf.sheriffEndpoint.get.timeout should be(1.second)
    conf.sheriffEndpoint.get.url.toString should be("http://localhost:8080/shrine-hms-authorization/queryAuthorization")
  }
  
  @Test
  def testApplyOptionalFields {
    val conf = entryPointServiceConfig("shrine-some-optional-props")
    
    conf.authenticationType should be(AuthenticationType.Pm)
    
    conf.authorizationType should be(AuthorizationType.NoAuthorization)
    
    conf.broadcasterIsLocal should be(true)
    conf.broadcasterServiceEndpoint should be(None)
    conf.sheriffCredentials should be(None)
    conf.sheriffEndpoint should be(None)
  }
}