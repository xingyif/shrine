package net.shrine.authentication

import com.typesafe.config.ConfigFactory
import net.shrine.util.ShouldMatchersForJUnit
import org.junit.Test

/**
 * @author david 
 * @since 8/12/15
 */
class ConfigUserSourceTest extends ShouldMatchersForJUnit {

  @Test
  def testIsAuthenticated {

    val config = ConfigFactory.load("admin")

    val userSource = ConfigUserSource.apply(config)

    userSource.qepUserName should equal("qep")
  }

}
