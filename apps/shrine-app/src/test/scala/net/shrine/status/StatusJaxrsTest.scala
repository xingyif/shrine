package net.shrine.status

import com.typesafe.config.ConfigFactory
import net.shrine.util.{ShouldMatchersForJUnit, SingleHubModel}
import org.json4s.native.Serialization
import org.json4s.{DefaultFormats, Formats}
import org.junit.Test

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

  @Test
  def testSummary() = {

    val summaryString = statusJaxrs.summary
    val summary = Serialization.read[Summary](summaryString)

    summary.adapterMappingsFileName.isDefined should be (true)
    summary.adapterMappingsDate.isEmpty should be (false)
  }

  //@Test
  def testQueryAdapterTest() = {

    val queryAdapterTestString = statusJaxrs.queryAdapterTest
    val queryAdapterTestResult = Serialization.read[QueryAdapterTestResult](queryAdapterTestString)

    queryAdapterTestResult.isHub should be (true)
    queryAdapterTestResult.adapterOk should be (true)
    queryAdapterTestResult.keystoreOk should be (true)
    queryAdapterTestResult.hubOk should be (false)
    queryAdapterTestResult.qepOk should be (true)
  }

  @Test
  def testI2b2() = {

    val i2b2String = statusJaxrs.i2b2
    val i2b2 = Serialization.read[I2b2](i2b2String)

    i2b2.crcUrl.isDefined should be (true)
  }

  @Test
  def testOptionalParts() = {

    val string = statusJaxrs.optionalParts

    val actual = Serialization.read[OptionalParts](string)

    actual.isHub should be (true)
    actual.stewardEnabled should be (true)
    actual.shouldQuerySelf should be (false)
    actual.downstreamNodes.size should be (3)
  }

  @Test
  def testHub() = {

    val string = statusJaxrs.hub

    val actual = Serialization.read[Hub](string)

    actual.create should be (true)
    actual.shouldQuerySelf should be (false)
    actual.downstreamNodes.size should be (3)
  }

  @Test
  def testQep() = {

    val string = statusJaxrs.qep
    val actual = Serialization.read[Qep](string)

    actual.create should be (true)
    actual.attachSigningCert should be (true)
    actual.authenticationType should be ("PmAuthenticator")
    actual.authorizationType should be ("StewardQueryAuthorizationService")
    actual.includeAggregateResults should be (false)
    actual.maxQueryWaitTimeMillis should be (300000000L)
    actual.trustModel should be (SingleHubModel(true).description)
    actual.trustModelIsHub should be (true)
    actual.broadcasterUrl shouldNot be (None)
  }

  @Test
  def testAdapter() = {

    val string = statusJaxrs.adapter

    val actual = Serialization.read[Adapter](string)

    actual.adapterLockoutAttemptsThreshold should be (0)
  }

  @Test
  def testKeyStore() = {
    val string = statusJaxrs.keystore
    val actual = Serialization.read[KeyStoreReport](string)
  }

}
