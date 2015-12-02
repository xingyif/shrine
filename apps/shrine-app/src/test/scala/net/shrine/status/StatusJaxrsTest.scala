package net.shrine.status

import com.typesafe.config.ConfigFactory
import net.shrine.util.ShouldMatchersForJUnit
import org.junit.Test

/**
  * Tests for StatusJaxrs
  *
  * @author david 
  * @since 12/2/15
  */
class StatusJaxrsTest extends ShouldMatchersForJUnit {

  @Test
  def testVersion = {
    val statusJaxrs = StatusJaxrs()

    val versionString = statusJaxrs.version

    println(s"versionString is $versionString")
  }

}
