package net.shrine.qep

import com.typesafe.config.ConfigFactory
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
    
    conf.includeAggregateResults should equal(false)

    conf.maxQueryWaitTime should equal(5.minutes)
  }
}