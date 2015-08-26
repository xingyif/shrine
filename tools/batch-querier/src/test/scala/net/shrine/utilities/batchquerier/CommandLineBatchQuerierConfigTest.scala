package net.shrine.utilities.batchquerier

import org.junit.Test
import net.shrine.protocol.AuthenticationInfo
import net.shrine.protocol.Credential
import net.shrine.utilities.scallop.{Keys => BaseKeys}
import BatchQuerierConfig.BatchQuerierConfigKeys
import java.io.File
import net.shrine.util.ShouldMatchersForJUnit

/**
 * @author clint
 * @date Oct 17, 2013
 */
//TODO
final class CommandLineBatchQuerierConfigTest extends ShouldMatchersForJUnit {
  val url = "http://example.com"
  val authn = AuthenticationInfo("some-domain", "some-user", Credential("some-password", false))
  val projectId = "FOO"
  val topicId = "who-knows"
  val queriesPerTerm = 5
    
  @Test
  def testParseAuthn {
    {
      val args = Seq("-c", authn.domain, authn.username, authn.credential.value, "-u", url)

      val conf = new CommandLineBatchQuerierConfig(args)

      conf.credentials() should equal(authn)
    }

    {
      val argsWithNoCredentials = Seq("-u", url, "-n", "123")

      val confWithNoCredentials = new CommandLineBatchQuerierConfig(argsWithNoCredentials)

      confWithNoCredentials.credentials.get should be(None)
    }
  }

  private def file(name: String): File = new File(name)
  
  private def allArgs(short: Boolean, showVersion: Boolean = true, showHelp: Boolean = true): Seq[String] = {
    Seq(
      (if (short) "-i" else "--input-file"), "foo.xml",
      (if (short) "-u" else "--url"), url,
      (if (short) "-c" else "--credentials"), authn.domain, authn.username, authn.credential.value,
      (if (short) "-o" else "--output-file"), "foo.csv",
      (if (short) "-p" else "--project-id"), projectId,
      (if (short) "-t" else "--topic-id"), topicId,
      (if (short) "-n" else "--queries-per-term"), queriesPerTerm.toString) ++
      (if(showVersion) Seq((if (short) "-v" else "--version")) else Nil)  ++
      (if(showHelp) Seq((if (short) "-h" else "--help")) else Nil)
  }

  @Test
  def testToTypesafeConfig {
    //No Args should make an empty config
    {
      val confWithNoArgs = new CommandLineBatchQuerierConfig(Nil)

      val config = confWithNoArgs.toTypesafeConfig

      config.entrySet.isEmpty should be(true)
    }

    //Some args
    def doSomeArgsTest() {
      val args = Seq("-c", authn.domain, authn.username, authn.credential.value, "-u", url)

      val conf = new CommandLineBatchQuerierConfig(args)

      val config = conf.toTypesafeConfig

      config.getString(s"${BatchQuerierConfigKeys.credentials}.${BaseKeys.domain}") should equal(authn.domain)
      config.getString(s"${BatchQuerierConfigKeys.credentials}.${BaseKeys.username}") should equal(authn.username)
      config.getString(s"${BatchQuerierConfigKeys.credentials}.${BaseKeys.password}") should equal(authn.credential.value)

      config.getString(BatchQuerierConfigKeys.shrineUrl) should equal(url)
      
      intercept[Exception] {
        config.getString(BatchQuerierConfigKeys.outputFile)
      }
    }

    //All args
    def doAllArgsTest(short: Boolean) {
      val args = allArgs(short)

      val config = (new CommandLineBatchQuerierConfig(args)).toTypesafeConfig

      config.getString(BatchQuerierConfigKeys.inputFile) should equal("foo.xml")
      config.getString(BatchQuerierConfigKeys.outputFile) should equal("foo.csv")
      config.getString(BatchQuerierConfigKeys.projectId) should equal(projectId)
      config.getString(s"${BatchQuerierConfigKeys.credentials}.${BaseKeys.domain}") should equal(authn.domain)
      config.getString(s"${BatchQuerierConfigKeys.credentials}.${BaseKeys.username}") should equal(authn.username)
      config.getString(s"${BatchQuerierConfigKeys.credentials}.${BaseKeys.password}") should equal(authn.credential.value)
      config.getString(BatchQuerierConfigKeys.shrineUrl) should equal(url)
      config.getInt(BatchQuerierConfigKeys.queriesPerTerm) should equal(queriesPerTerm)
    }

    doSomeArgsTest()
    
    doAllArgsTest(true)
    doAllArgsTest(false)
  }

  @Test
  def testParse {
    def doAllArgsTest(short: Boolean) {
      val args = allArgs(short)

      val config = new CommandLineBatchQuerierConfig(args)

      config.inputFile() should equal(file("foo.xml"))
      config.outputFile() should equal(file("foo.csv"))
      config.projectId() should equal(projectId)
      config.topicId() should equal(topicId)
      config.credentials() should equal(authn)
      config.url() should equal(url)
      config.queriesPerTerm() should equal(queriesPerTerm)
    }

    doAllArgsTest(true)
    doAllArgsTest(false)

    //No Args
    {
      val config = new CommandLineBatchQuerierConfig(Nil)

      config.inputFile.get should be(None)
      config.outputFile.get should be(None)
      config.projectId.get should be(None)
      config.topicId.get should be(None)
      config.credentials.get should be(None)
      config.url.get should be(None)
      config.outputFile.get should be(None)
      config.queriesPerTerm.get should equal(None)
    }
  }

  @Test
  def testVersionToggle {
    def doTestVersionToggle(showVersion: Boolean) {
      val args = allArgs(true, showVersion)

      val config = new CommandLineBatchQuerierConfig(args)
      
      config.showVersionToggle.isSupplied should be(showVersion)
    }
    
    doTestVersionToggle(true)
    doTestVersionToggle(false)
  }
  
  @Test
  def testHelpToggle {
    def doTestHelpToggle(help: Boolean) {
      val args = allArgs(true, showVersion = false, showHelp = help)

      val config = new CommandLineBatchQuerierConfig(args)
      
      config.showVersionToggle.isSupplied should be(false)
      config.showHelpToggle.isSupplied should be(help)
    }
    
    doTestHelpToggle(true)
    doTestHelpToggle(false)
  }
}