package net.shrine.utilities.scanner

import net.shrine.util.ShouldMatchersForJUnit
import org.junit.Test
import net.shrine.protocol.AuthenticationInfo
import net.shrine.protocol.Credential

/**
 * @author clint
 * @date May 2, 2013
 */
final class HasClasspathAndCommandLineAndScannerConfigTest extends ShouldMatchersForJUnit {
  @Test
  def testCommandLineTrumpsConfigFile {
    import scala.concurrent.duration._
    
    val adapterMappingsFile = "from-command-line.xml"
    val ontologySqlFile = "from-command-line.sql"
	val reScanTimeout = 42.minutes
	val rescanTimeoutForCommandLine = Seq("42", "minutes")
	val shrineUrl = "http://example.com/from-command-line"
	val projectId = "FROM_COMMAND_LINE"
	val authorization = AuthenticationInfo("command-line-domain", "command-line-user", Credential("command-line-password", false))
    val outputFile = "from-command-line.csv"
    
    val args = Seq("-a", adapterMappingsFile,
    			   "-s", ontologySqlFile,
    			   "-t") ++ rescanTimeoutForCommandLine ++ Seq(
    			   "-u", shrineUrl,
    			   "-p", projectId,
    			   "-c") ++ Seq(authorization.domain, authorization.username, authorization.credential.value) ++ Seq(
    			   "-o", outputFile)
      
    val module = ClasspathAndCommandLineScannerConfigSource
    			   
    val commandLineProps = CommandLineScannerConfigParser(args)
    
    val config = module.config(commandLineProps)
    
    config.adapterMappingsFile should equal(adapterMappingsFile)
    config.ontologySqlFile should equal(ontologySqlFile)
    config.reScanTimeout should equal(reScanTimeout)
    config.shrineUrl should equal(shrineUrl)
    config.projectId should equal(projectId)
    config.authorization should equal(authorization)
    config.outputFile should equal(outputFile)
  }
}