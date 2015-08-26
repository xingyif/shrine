package net.shrine.utilities.scanner

import net.shrine.util.ShouldMatchersForJUnit
import org.junit.Test
import scala.concurrent.duration._
import net.shrine.protocol.AuthenticationInfo
import net.shrine.protocol.Credential
import ScannerConfig.ScannerConfigKeys
import net.shrine.utilities.scallop.{Keys => BaseKeys}

/**
 * @author clint
 * @date Mar 26, 2013
 */
final class CommandLineScannerConfigParserTest extends ShouldMatchersForJUnit {
  val url = "http://example.com"
    
  val pmUrl = "http://example.com/pm"

  val authn = AuthenticationInfo("some-domain", "some-user", Credential("some-password", false))

  val projectId = "FOO"

  @Test
  def testParseRescanTimeout {
    def doTimeoutTest(short: Boolean, howMany: Int, timeUnit: String, toDuration: Int => Duration) {
      val args = Seq(if (short) "-t" else "--rescan-timeout", howMany.toString, timeUnit, "-c", authn.domain, authn.username, authn.credential.value, "-u", url)

      val conf = new CommandLineScannerConfigParser(args)

      conf.rescanTimeout() should equal(toDuration(howMany))

      conf.url() should equal(url)

      conf.credentials() should equal(authn)
    }

    doTimeoutTest(true, 42, "minutes", _.minutes)

    doTimeoutTest(false, 42, "minutes", _.minutes)

    doTimeoutTest(true, 17, "seconds", _.seconds)

    doTimeoutTest(false, 17, "seconds", _.seconds)

    doTimeoutTest(true, 123, "milliseconds", _.milliseconds)

    doTimeoutTest(false, 123, "milliseconds", _.milliseconds)
  }
  
  @Test
  def testParseMaxWaitTime {
    def doTimeoutTest(short: Boolean, howMany: Int, timeUnit: String, toDuration: Int => Duration) {
      val args = Seq(if (short) "-w" else "--max-wait-time", howMany.toString, timeUnit, "-c", authn.domain, authn.username, authn.credential.value, "-u", url)

      val conf = new CommandLineScannerConfigParser(args)

      conf.maxTimeToWaitForResults() should equal(toDuration(howMany))

      conf.url() should equal(url)

      conf.credentials() should equal(authn)
    }

    doTimeoutTest(true, 42, "minutes", _.minutes)

    doTimeoutTest(false, 42, "minutes", _.minutes)

    doTimeoutTest(true, 17, "seconds", _.seconds)

    doTimeoutTest(false, 17, "seconds", _.seconds)

    doTimeoutTest(true, 123, "milliseconds", _.milliseconds)

    doTimeoutTest(false, 123, "milliseconds", _.milliseconds)
  }

  @Test
  def testParseAuthn {
    {
      val args = Seq("-c", authn.domain, authn.username, authn.credential.value, "-u", url, "-t", "123", "minutes")

      val conf = new CommandLineScannerConfigParser(args)

      conf.credentials() should equal(authn)
    }

    {
      val argsWithNoCredentials = Seq("-u", url, "-t", "123", "minutes")

      val confWithNoCredentials = new CommandLineScannerConfigParser(argsWithNoCredentials)

      confWithNoCredentials.credentials.get should be(None)
    }
  }

  private def allArgs(short: Boolean, showVersion: Boolean = true, showHelp: Boolean = true): Seq[String] = {
    Seq(
      (if (short) "-r" else "--pm-url"), pmUrl,
      (if (short) "-a" else "--adapter-mappings-file"), "foo.xml",
      (if (short) "-s" else "--ontology-sql-file"), "foo.sql",
      (if (short) "-t" else "--rescan-timeout"), "5", "seconds",
      (if (short) "-w" else "--max-wait-time"), "42", "minutes",
      (if (short) "-p" else "--project-id"), projectId,
      (if (short) "-c" else "--credentials"), authn.domain, authn.username, authn.credential.value,
      (if (short) "-u" else "--url"), url,
      (if (short) "-o" else "--output-file"), "blarg.csv") ++ 
      (if(showVersion) Seq((if (short) "-v" else "--version")) else Nil)  ++
      (if(showHelp) Seq((if (short) "-h" else "--help")) else Nil)
  }

  @Test
  def testToTypesafeConfig {
    //No Args should make an empty config
    {
      val confWithNoArgs = new CommandLineScannerConfigParser(Nil)

      val config = confWithNoArgs.toTypesafeConfig

      config.entrySet.isEmpty should be(true)
    }

    //Some args
    def doSomeArgsTest(timeout: Int, timeUnit: String, toDuration: Int => Duration) {
      val args = Seq("-t", timeout.toString, timeUnit, "-c", authn.domain, authn.username, authn.credential.value, "-u", url)

      val conf = new CommandLineScannerConfigParser(args)

      val config = conf.toTypesafeConfig

      conf.rescanTimeout() should equal(toDuration(timeout))

      config.getInt(s"${ScannerConfigKeys.reScanTimeout}.$timeUnit") should equal(timeout)

      config.getString(s"${ScannerConfigKeys.credentials}.${BaseKeys.domain}") should equal(authn.domain)
      config.getString(s"${ScannerConfigKeys.credentials}.${BaseKeys.username}") should equal(authn.username)
      config.getString(s"${ScannerConfigKeys.credentials}.${BaseKeys.password}") should equal(authn.credential.value)

      config.getString(ScannerConfigKeys.shrineUrl) should equal(url)
      
      intercept[Exception] {
        config.getString(ScannerConfigKeys.outputFile)
      }
      
      intercept[Exception] {
    	config.getString(ScannerConfigKeys.pmUrl)
      }
    }

    //All args
    def doAllArgsTest(short: Boolean) {
      val args = allArgs(short)

      val config = (new CommandLineScannerConfigParser(args)).toTypesafeConfig

      config.getString(ScannerConfigKeys.adapterMappingsFile) should equal("foo.xml")
      config.getString(ScannerConfigKeys.ontologySqlFile) should equal("foo.sql")
      config.getInt(s"${ScannerConfigKeys.reScanTimeout}.seconds") should equal(5)
      config.getInt(s"${ScannerConfigKeys.maxTimeToWaitForResults}.minutes") should equal(42)
      config.getString(ScannerConfigKeys.projectId) should equal(projectId)
      config.getString(s"${ScannerConfigKeys.credentials}.${BaseKeys.domain}") should equal(authn.domain)
      config.getString(s"${ScannerConfigKeys.credentials}.${BaseKeys.username}") should equal(authn.username)
      config.getString(s"${ScannerConfigKeys.credentials}.${BaseKeys.password}") should equal(authn.credential.value)
      config.getString(ScannerConfigKeys.shrineUrl) should equal(url)
      config.getString(ScannerConfigKeys.outputFile) should equal("blarg.csv")
      config.getString(ScannerConfigKeys.pmUrl) should equal(pmUrl)
    }

    doSomeArgsTest(123, BaseKeys.milliseconds, _.milliseconds)
    doSomeArgsTest(42, BaseKeys.seconds, _.seconds)
    doSomeArgsTest(99, BaseKeys.minutes, _.minutes)
    
    doAllArgsTest(true)
    doAllArgsTest(false)
  }

  @Test
  def testParse {
    def doAllArgsTest(short: Boolean) {
      val args = allArgs(short)

      val config = new CommandLineScannerConfigParser(args)

      config.adapterMappingsFile() should equal("foo.xml")
      config.ontologySqlFile() should equal("foo.sql")
      config.rescanTimeout() should equal(5.seconds)
      config.maxTimeToWaitForResults() should equal(42.minutes)
      config.projectId() should equal(projectId)
      config.credentials() should equal(authn)
      config.url() should equal(url)
      config.outputFile() should equal("blarg.csv")
    }

    doAllArgsTest(true)
    doAllArgsTest(false)

    //No Args
    {
      val config = new CommandLineScannerConfigParser(Nil)

      config.adapterMappingsFile.get should be(None)
      config.ontologySqlFile.get should be(None)
      config.rescanTimeout.get should be(None)
      config.maxTimeToWaitForResults.get should be(None)
      config.projectId.get should be(None)
      config.credentials.get should be(None)
      config.url.get should be(None)
      config.outputFile.get should be(None)
    }
  }

  @Test
  def testVersionToggle {
    def doTestVersionToggle(showVersion: Boolean) {
      val args = allArgs(true, showVersion)

      val config = new CommandLineScannerConfigParser(args)
      
      config.showVersionToggle.isSupplied should be(showVersion)
    }
    
    doTestVersionToggle(true)
    doTestVersionToggle(false)
  }
  
  @Test
  def testHelpToggle {
    def doTestHelpToggle(help: Boolean) {
      val args = allArgs(true, showVersion = false, showHelp = help)

      val config = new CommandLineScannerConfigParser(args)
      
      config.showVersionToggle.isSupplied should be(false)
      config.showHelpToggle.isSupplied should be(help)
    }
    
    doTestHelpToggle(true)
    doTestHelpToggle(false)
  }
}