package net.shrine.utilities.batchquerier

import java.io.File
import net.shrine.protocol.{ResultOutputTypes, AuthenticationInfo, Credential, ResultOutputType}
import com.typesafe.config.Config
import com.typesafe.config.ConfigException
import net.shrine.utilities.scallop.{Keys => BaseKeys}
import scala.util.Try
import net.shrine.config.Keys

/**
 * @author clint
 * @since Sep 6, 2013
 */
final case class BatchQuerierConfig(
    expressionFile: File,
    shrineUrl: String,
    authorization: AuthenticationInfo,
    outputFile: File,
    projectId: String,
    topicId: String,
    queriesPerTerm: Int,
    breakdownTypes: Set[ResultOutputType])
    
object BatchQuerierConfig {

  def apply(config: Config): BatchQuerierConfig = {
    def file(path: String) = new File(path)
    
    def getAuthInfo(subConfig: Config): AuthenticationInfo = {
      import BaseKeys._
      
      def string(path: String) = subConfig.getString(path)
      
      def requirePath(path: String) = if (!subConfig.hasPath(path)) throw new ConfigException.Missing(s"Expected to find '$path' in $subConfig")

      requirePath(domain)
      requirePath(username)
      requirePath(password)
      
      AuthenticationInfo(string(domain), string(username), Credential(string(password), false))
    }
    
    def string(path: String) = config.getString(path)
    
    def intOption(path: String) = if(config.hasPath(path)) Option(config.getInt(path)) else None
    
    import BatchQuerierConfigKeys._
    
    BatchQuerierConfig(
        file(string(inputFile)),
        string(shrineUrl),
        getAuthInfo(config.getConfig(credentials)),
        file(string(outputFile)),
        string(projectId),
        string(topicId),
        intOption(queriesPerTerm).getOrElse(Defaults.queriesPerTerm),
        Try(ResultOutputTypes.fromConfig(config.getConfig(breakdownResultOutputTypes))).getOrElse(Set.empty))
  }
  
  object Defaults {
    val queriesPerTerm = 3
  }
  
  object BatchQuerierConfigKeys {
    private val base = "batch"
      
    private def subKey(k: String) = BaseKeys.subKey(base)(k)
    
    val inputFile = subKey("inputFile")
    val outputFile = subKey("outputFile")
    val shrineUrl = subKey("shrineUrl")
    val credentials = BaseKeys.credentials(base)
    val projectId = subKey("projectId")
    val topicId = subKey("topicId")
    val queriesPerTerm = subKey("queriesPerTerm")
    val breakdownResultOutputTypes = "breakdownResultOutputTypes"
  }
}