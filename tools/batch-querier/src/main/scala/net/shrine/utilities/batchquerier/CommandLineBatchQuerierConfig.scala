package net.shrine.utilities.batchquerier

import net.shrine.utilities.scallop.ValueConverters
import scala.concurrent.duration.Duration
import net.shrine.protocol.AuthenticationInfo
import com.typesafe.config.Config
import java.io.File
import com.typesafe.config.ConfigFactory
import org.rogach.scallop.Scallop
import net.shrine.utilities.scallop.AbstractArgParser

/**
 * @author clint
 * @date Oct 17, 2013
 */
final case class CommandLineBatchQuerierConfig(arguments: Seq[String]) extends AbstractArgParser(arguments) {
  
  import ValueConverters.Implicits._
  
  val inputFile = opt[File](required = false)
  val url = opt[String](required = false)
  val credentials = opt[AuthenticationInfo](required = false)
  val outputFile = opt[File](required = false)
  val projectId = opt[String](required = false)
  val topicId = opt[String](required = false)
  val queriesPerTerm = opt[Int](short = 'n', required = false)
  
  def toTypesafeConfig: Config = {
    
    import BatchQuerierConfig.BatchQuerierConfigKeys

    def credentialKey(k: String) = s"${BatchQuerierConfigKeys.credentials}.$k"

    import BatchQuerierConfig.BatchQuerierConfigKeys
    import net.shrine.utilities.scallop.{Keys => BaseKeys}
    
    val exceptTimeout = Map(
      BatchQuerierConfigKeys.inputFile -> inputFile.get.map(_.getPath),
      BatchQuerierConfigKeys.outputFile -> outputFile.get.map(_.getPath),
      BatchQuerierConfigKeys.projectId -> projectId.get,
      BatchQuerierConfigKeys.topicId -> topicId.get,
      BatchQuerierConfigKeys.shrineUrl -> url.get,
      BatchQuerierConfigKeys.queriesPerTerm -> queriesPerTerm.get,
      credentialKey(BaseKeys.domain) -> credentials.get.map(_.domain),
      credentialKey(BaseKeys.username) -> credentials.get.map(_.username),
      credentialKey(BaseKeys.password) -> credentials.get.map(_.credential.value))

    import scala.collection.JavaConverters._
    
    ConfigFactory.parseMap(exceptTimeout.collect { case (k, Some(v)) => (k, v) }.asJava)
  }
}
