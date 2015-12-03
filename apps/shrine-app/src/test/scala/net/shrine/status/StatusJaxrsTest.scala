package net.shrine.status

import com.typesafe.config.{ConfigValue, ConfigFactory}
import net.shrine.util.ShouldMatchersForJUnit
import org.json4s.{DefaultFormats, Formats}
import org.junit.Test
import org.json4s.native.Serialization

import scala.collection.immutable.Map

/**
  * Tests for StatusJaxrs
  *
  * @author david 
  * @since 12/2/15
  */
class StatusJaxrsTest extends ShouldMatchersForJUnit {

  implicit def json4sFormats: Formats = DefaultFormats
  val expectedConfig = ConfigFactory.load("shrine")
  val statusJaxrs = StatusJaxrs(expectedConfig)

  @Test
  def testVersion() = {
    val versionString = statusJaxrs.version
    val version = Serialization.read[Version](versionString)

    version should equal(Version("changeMe"))
  }

  @Test
  def testConfig() = {
    val expectedJson4sConfig = Json4sConfig(expectedConfig)

    val configString = statusJaxrs.config

    val config = Serialization.read[Json4sConfig](configString)

    config should equal(expectedJson4sConfig)

    val passwordKeys = config.keyValues.filter(x => Json4sConfig.isPassword(x._1))

    passwordKeys should equal(Map.empty[String,String])
  }
}