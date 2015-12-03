package net.shrine.broadcaster

import com.typesafe.config.ConfigFactory
import net.shrine.config.Keys
import net.shrine.util.ShouldMatchersForJUnit
import org.junit.Test

/**
 * @author clint
 * @since Jan 20, 2014
 */
final class HubConfigTest extends ShouldMatchersForJUnit {

  private def hubConfig(baseFileName: String) = HubConfig(ConfigFactory.load(baseFileName).getConfig(s"shrine.${Keys.hub}"))
  
  @Test
  def testApply {
    val conf = hubConfig("shrine")

    import scala.concurrent.duration._
    import NodeListParserTest.node

    conf.maxQueryWaitTime should equal(4.5.minutes)
    
    conf.downstreamNodes.toSet should equal {
      Set(
        node("some hospital", "http://example.com/foo"),
        node("CHB", "http://example.com/chb"),
        node("PHS", "http://example.com/phs"))
    }
  }
  
  @Test
  def testApplyOptionalFields {
    val conf = hubConfig("shrine-some-optional-props")
    
    conf.shouldQuerySelf should be(HubConfig.defaultShouldQuerySelf)
    conf.downstreamNodes should equal(Nil)
  }
}