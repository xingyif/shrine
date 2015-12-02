package net.shrine.status

import com.typesafe.config.ConfigFactory
import net.shrine.util.ShouldMatchersForJUnit
import org.json4s.{DefaultFormats, Formats}
import org.junit.Test
import org.json4s.native.Serialization

/**
  * Tests for StatusJaxrs
  *
  * @author david 
  * @since 12/2/15
  */
class StatusJaxrsTest extends ShouldMatchersForJUnit {

  implicit def json4sFormats: Formats = DefaultFormats

  @Test
  def testVersion = {
    val statusJaxrs = StatusJaxrs()

    val versionString = statusJaxrs.version

    val version = Serialization.read[Version](versionString)

    version should equal(Version("changeMe"))
  }

}
