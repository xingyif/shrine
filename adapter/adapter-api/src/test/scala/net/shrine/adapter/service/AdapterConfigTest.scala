package net.shrine.adapter.service

import com.typesafe.config.ConfigFactory
import net.shrine.config.Keys
import net.shrine.util.ShouldMatchersForJUnit
import org.junit.Test

import net.shrine.client.EndpointConfigTest

/**
 * @author clint
 * @since Jan 20, 2014
 */
final class AdapterConfigTest extends ShouldMatchersForJUnit {
  private def getConf(name: String): AdapterConfig = {

    AdapterConfig(ConfigFactory.load(name).getConfig(s"shrine.adapter"))
  }
  
  @Test
  def testApply {
    import EndpointConfigTest.endpoint

    val conf = getConf("shrine")

    conf.crcEndpoint should equal(endpoint("http://services.i2b2.org/i2b2/rest/QueryToolService/"))

    conf.setSizeObfuscation should equal(true)

    conf.adapterLockoutAttemptsThreshold should equal(10)

    conf.adapterMappingsFileName should equal("AdapterMappings.xml")
	
	import scala.concurrent.duration._

    conf.maxSignatureAge should equal(5.minutes)
	
	conf.immediatelyRunIncomingQueries should be(false)
  }
  
  @Test
  def testApplyOptionalFields {
    val conf = getConf("shrine-some-optional-props")
    
    conf.immediatelyRunIncomingQueries should be(AdapterConfig.defaultImmediatelyRunIncomingQueries)
  }
}