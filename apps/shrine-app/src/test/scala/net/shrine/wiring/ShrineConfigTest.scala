package net.shrine.wiring

import com.typesafe.config.ConfigFactory
import net.shrine.authentication.AuthenticationType
import net.shrine.authorization.AuthorizationType
import net.shrine.broadcaster.NodeListParserTest
import net.shrine.client.EndpointConfigTest
import net.shrine.crypto.SigningCertStrategy
import net.shrine.util.ShouldMatchersForJUnit
import org.junit.Test

/**
 * @author clint
 * @since Feb 6, 2013
 */
final class ShrineConfigTest extends ShouldMatchersForJUnit  {
  private def shrineConfig(baseFileName: String, loadBreakdownsFile: Boolean = true) = {
    val baseConfig = ConfigFactory.load(baseFileName)
    val breakdownConfig = ConfigFactory.load("breakdowns")
    
    val config = if(loadBreakdownsFile) baseConfig.withFallback(breakdownConfig) else baseConfig
    
    ShrineConfig(config)
  }
  
  import scala.concurrent.duration._
  
  @Test
  def testApply() {
    import NodeListParserTest.node
    import EndpointConfigTest.endpoint

    val conf = shrineConfig("shrine")

    conf.queryEntryPointConfig.get.authenticationType should be(AuthenticationType.Ecommons)
    
    conf.queryEntryPointConfig.get.authorizationType should be(AuthorizationType.HmsSteward)
    
    conf.queryEntryPointConfig.get.sheriffEndpoint.get should equal(endpoint("http://localhost:8080/shrine-hms-authorization/queryAuthorization"))
    
    conf.queryEntryPointConfig.get.sheriffCredentials.get.domain should be(None)
    conf.queryEntryPointConfig.get.sheriffCredentials.get.username should be("sheriffUsername")
    conf.queryEntryPointConfig.get.sheriffCredentials.get.password should be("sheriffPassword")

    conf.crcHiveCredentials should equal(conf.pmHiveCredentials)
    
    conf.crcHiveCredentials.domain should equal("HarvardDemo")
    conf.crcHiveCredentials.username should equal("demo")
    conf.crcHiveCredentials.password should equal("demouser")
    conf.crcHiveCredentials.projectId should equal("Demo")
    
    conf.ontHiveCredentials.domain should equal("HarvardDemo")
    conf.ontHiveCredentials.username should equal("demo")
    conf.ontHiveCredentials.password should equal("demouser")
    conf.ontHiveCredentials.projectId should equal("SHRINE")
    
    conf.ontHiveCredentials.domain should equal("HarvardDemo")
    conf.ontHiveCredentials.username should equal("demo")
    conf.ontHiveCredentials.password should equal("demouser")
    conf.ontHiveCredentials.projectId should equal("SHRINE")
    
    conf.queryEntryPointConfig.get.broadcasterIsLocal should be(false)
    conf.queryEntryPointConfig.get.broadcasterServiceEndpoint.get should equal(endpoint("http://example.com/shrine/rest/broadcaster/broadcast"))
    conf.queryEntryPointConfig.get.maxQueryWaitTime should equal(5.minutes)
    conf.queryEntryPointConfig.get.signingCertStrategy should equal(SigningCertStrategy.Attach)
    
    conf.queryEntryPointConfig.get.includeAggregateResults should equal(false)

    conf.hubConfig.get.maxQueryWaitTime should equal(4.5.minutes)
    
    conf.adapterStatusQuery should equal("""\\SHRINE\SHRINE\Diagnoses\Mental Illness\Disorders usually diagnosed in infancy, childhood, or adolescence\Pervasive developmental disorders\Infantile autism, current or active state\""")
    
    conf.hubConfig.get.downstreamNodes.toSet should equal {
      Set(
        node("some hospital", "http://example.com/foo"),
        node("CHB", "http://example.com/chb"),
        node("PHS", "http://example.com/phs"))
    }
  }
  
  @Test
  def testApplyOptionalFields() {
    val conf = shrineConfig("shrine-no-optional-configs", loadBreakdownsFile = false)

    conf.hubConfig should be(None)
    conf.queryEntryPointConfig should be(None)
  }
  
  @Test
  def testApplySomeOptionalFields() {
    val conf = shrineConfig("shrine-some-optional-props")
    
    conf.queryEntryPointConfig.get.authenticationType should be(AuthenticationType.Pm)
    conf.queryEntryPointConfig.get.authorizationType should be(AuthorizationType.NoAuthorization)
    conf.queryEntryPointConfig.get.signingCertStrategy should be(SigningCertStrategy.DontAttach)
    
  }
}