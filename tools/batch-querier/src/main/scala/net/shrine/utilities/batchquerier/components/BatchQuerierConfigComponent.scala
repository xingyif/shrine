package net.shrine.utilities.batchquerier.components

import net.shrine.log.Loggable
import net.shrine.utilities.batchquerier.BatchQuerierConfig
import com.typesafe.config.ConfigFactory
import net.shrine.utilities.batchquerier.HasBatchQuerierConfig
import net.shrine.utilities.batchquerier.CommandLineBatchQuerierConfig
import scala.util.Try

/**
 * @author clint
 * @date Sep 6, 2013
 */
trait BatchQuerierConfigComponent extends HasBatchQuerierConfig {
  protected val args: Seq[String]
  
  private lazy val fromClassPath = ConfigFactory.load()
  
  lazy val commandLineArgs = CommandLineBatchQuerierConfig(args)
  
  private lazy val fromCommandLine = commandLineArgs.toTypesafeConfig
  
  private lazy val mergedConfig = fromCommandLine.withFallback(fromClassPath)
  
  lazy val config: BatchQuerierConfig = BatchQuerierConfig(mergedConfig)
  
  def printUsage() {
    commandLineArgs.printHelp()
  }
  
  def configProvided: Either[String, Boolean] = {
    def has(path: String): Either[String, Boolean] = {
      val error = Left(s"Config value '$path' is missing")
      
      Try(mergedConfig.hasPath(path)).map(b => if(b) Right(b) else error).getOrElse(error)
    }
    
    import BatchQuerierConfig.BatchQuerierConfigKeys
    
    for {
      hasCredentials <- has(BatchQuerierConfigKeys.credentials).right
      hasInputFile <- has(BatchQuerierConfigKeys.inputFile).right
      hasOutputFile <- has(BatchQuerierConfigKeys.outputFile).right
      hasProjectId <- has(BatchQuerierConfigKeys.projectId).right
      hasUrl <- has(BatchQuerierConfigKeys.shrineUrl).right
      hasTopicId <- has(BatchQuerierConfigKeys.topicId).right
    } yield hasCredentials && hasInputFile && hasOutputFile && hasProjectId && hasUrl && hasTopicId
  }
}