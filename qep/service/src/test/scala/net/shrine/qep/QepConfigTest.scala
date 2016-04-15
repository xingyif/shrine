package net.shrine.qep

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
  def testApply() {
    val conf: QepConfig = entryPointServiceConfig("shrine")

    import scala.concurrent.duration._
    
    conf.broadcasterIsLocal should be(false)
    
    conf.broadcasterServiceEndpoint.get.acceptAllCerts should be(true)
    conf.broadcasterServiceEndpoint.get.timeout should be(1.second)
    conf.broadcasterServiceEndpoint.get.url.toString should equal("http://example.com/shrine/rest/broadcaster/broadcast")
    
    conf.includeAggregateResults should equal(false)

    conf.maxQueryWaitTime should equal(5.minutes)
  }
  
  @Test
  def testApplyOptionalFields() {
    val conf = entryPointServiceConfig("shrine-some-optional-props")
    
    conf.broadcasterIsLocal should be(true)
    conf.broadcasterServiceEndpoint should be(None)
  }
}