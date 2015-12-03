package net.shrine.utilities.scanner

import net.shrine.util.ShouldMatchersForJUnit
import org.junit.Test
import com.typesafe.config.ConfigFactory
import com.typesafe.config.Config
import com.typesafe.config.ConfigException
import net.shrine.crypto.KeyStoreDescriptor
import net.shrine.crypto.KeyStoreType

/**
 * @author clint
 * @date Mar 8, 2013
 */
final class ScannerConfigTest extends ShouldMatchersForJUnit {
  import scala.concurrent.duration._

  @Test
  def testFromConfig {
    val fromClassPath = ConfigFactory.load

    {
      val scannerConfig = ScannerConfig(fromClassPath)

      scannerConfig should not be (null)
      scannerConfig.adapterMappingsFile should be("testAdapterMappings.xml")
      scannerConfig.ontologySqlFile should be("testShrineWithSyns.sql")
      scannerConfig.reScanTimeout should be(99.seconds)
      scannerConfig.maxTimeToWaitForResults should be(123.minutes)
      scannerConfig.shrineUrl should be("https://example.com")
      scannerConfig.projectId should be("SHRINE-PROJECT")
      scannerConfig.authorization should not be (null)
      scannerConfig.authorization.domain should be("TestDomain")
      scannerConfig.authorization.username should be("testuser")
      scannerConfig.authorization.credential.value should be("testpassword")
      scannerConfig.authorization.credential.isToken should be(false)
      scannerConfig.outputFile should equal("foo.csv")
      scannerConfig.keystoreDescriptor.caCertAliases should equal(Seq("carra ca"))
      scannerConfig.keystoreDescriptor should equal(KeyStoreDescriptor("shrine.keystore", "chiptesting", Some("test-cert"), Seq("carra ca"), KeyStoreType.PKCS12))
      scannerConfig.pmUrl should equal("http://example.com/pm")
    }

    {
      val scannerConfig = ScannerConfig(fromClassPath.withoutPath(ScannerConfig.ScannerConfigKeys.outputFile))

      scannerConfig.outputFile should equal(FileNameSource.nextOutputFileName)
    }
  }

  @Test
  def testGetDuration {
    intercept[ConfigException.Missing] {
      ScannerConfig.getDuration("", ConfigFactory.empty())
    }

    import scala.collection.JavaConverters._

    import net.shrine.utilities.scallop.Keys.{ milliseconds, seconds, minutes }

    ScannerConfig.getDuration("", ConfigFactory.parseMap(Map(milliseconds -> "123").asJava)) should be(123.milliseconds)

    ScannerConfig.getDuration("", ConfigFactory.parseMap(Map(seconds -> "123").asJava)) should be(123.seconds)

    ScannerConfig.getDuration("", ConfigFactory.parseMap(Map(minutes -> "123").asJava)) should be(123.minutes)

    //Priority should be millis > seconds > minutes

    ScannerConfig.getDuration("", ConfigFactory.parseMap(Map(milliseconds -> "123", seconds -> "456", minutes -> "789").asJava)) should be(123.milliseconds)

    ScannerConfig.getDuration("", ConfigFactory.parseMap(Map(seconds -> "456", minutes -> "789").asJava)) should be(456.seconds)
  }
}