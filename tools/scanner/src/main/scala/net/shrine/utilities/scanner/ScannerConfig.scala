package net.shrine.utilities.scanner

import scala.concurrent.duration.Duration
import net.shrine.protocol.{ResultOutputTypes, AuthenticationInfo, Credential, ResultOutputType}
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigException
import scala.util.Try
import net.shrine.utilities.scallop.{Keys => BaseKeys}
import net.shrine.crypto.{KeyStoreDescriptorParser, KeyStoreDescriptor}
import net.shrine.config.Keys

/**
 * @author clint
 * @since Mar 6, 2013
 */
final case class ScannerConfig(
  adapterMappingsFile: String,
  ontologySqlFile: String,
  reScanTimeout: Duration,
  maxTimeToWaitForResults: Duration,
  shrineUrl: String,
  projectId: String,
  authorization: AuthenticationInfo,
  outputFile: String,
  keystoreDescriptor: KeyStoreDescriptor,
  pmUrl: String,
  breakdownTypes: Set[ResultOutputType])

object ScannerConfig {
  private[scanner] def getDuration(keyName: String, subConfig: Config): Duration = {
    import scala.concurrent.duration._
    import ScannerConfigKeys._
    import net.shrine.utilities.scallop.Keys._

    if (subConfig.hasPath(milliseconds)) { subConfig.getLong(milliseconds).milliseconds }
    else if (subConfig.hasPath(seconds)) { subConfig.getLong(seconds).seconds }
    else if (subConfig.hasPath(minutes)) { subConfig.getLong(minutes).minutes }
    else { throw new ConfigException.Missing(s"Expected to find one of $keyName.{${milliseconds}, ${seconds}, ${minutes}} at subConfig $subConfig") }
  }

  def apply(config: Config): ScannerConfig = {
    def getAuthInfo(subConfig: Config): AuthenticationInfo = {
      import BaseKeys._
      
      def requirePath(path: String) = if (!subConfig.hasPath(path)) throw new ConfigException.Missing(s"Expected to find '$path' in $subConfig")

      requirePath(domain)
      requirePath(username)
      requirePath(password)

      AuthenticationInfo(subConfig.getString(domain), subConfig.getString(username), Credential(subConfig.getString(password), false))
    }

    import ScannerConfigKeys._
    
    def string(k: String) = config.getString(k)
    
    def duration(k: String) = getDuration(k, config.getConfig(k))
    
    def authInfo(k: String) = getAuthInfo(config.getConfig(k))
    
    ScannerConfig(
      string(adapterMappingsFile),
      string(ontologySqlFile),
      duration(reScanTimeout),
      duration(maxTimeToWaitForResults),
      string(shrineUrl),
      string(projectId),
      authInfo(credentials),
      Try(string(outputFile)).getOrElse(FileNameSource.nextOutputFileName),
      KeyStoreDescriptorParser(config.getConfig(keystore)),
      string(pmUrl),
      Try(ResultOutputTypes.fromConfig(config.getConfig(breakdownResultOutputTypes))).getOrElse(Set.empty))
  }

  object ScannerConfigKeys {
    val base = "scanner"
    
    private def subKey(k: String) = BaseKeys.subKey(base)(k)

    val keystore = subKey("keystore")
    val adapterMappingsFile = subKey("adapterMappingsFile")
    val ontologySqlFile = subKey("ontologySqlFile")
    val reScanTimeout = subKey("reScanTimeout")
    val maxTimeToWaitForResults = subKey("maxTimeToWaitForResults")
    val shrineUrl = subKey("shrineUrl")
    val pmUrl = subKey("pmUrl")
    val projectId = subKey("projectId")
    val credentials = BaseKeys.credentials(base)
    
    val breakdownResultOutputTypes = subKey("breakdownResultOutputTypes")
    
    val outputFile = subKey("outputFile")
  }
}
