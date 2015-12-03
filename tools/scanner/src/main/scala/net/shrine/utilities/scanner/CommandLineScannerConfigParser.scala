package net.shrine.utilities.scanner

import scala.concurrent.duration.Duration
import org.rogach.scallop.ValueConverter
import org.rogach.scallop.ArgType
import scala.util.Try
import net.shrine.protocol.AuthenticationInfo
import net.shrine.protocol.Credential
import scala.reflect.runtime.universe._
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import org.rogach.scallop.Scallop
import java.util.concurrent.TimeUnit
import net.shrine.utilities.scallop.ManifestValueConverter
import net.shrine.utilities.scallop.Parser
import net.shrine.utilities.scallop.ValueConverters
import net.shrine.utilities.scallop.AbstractArgParser

/**
 * @author clint
 * @date Mar 26, 2013
 */
final case class CommandLineScannerConfigParser(arguments: Seq[String]) extends AbstractArgParser(arguments) {
  override protected def onError(e: Throwable) = CommandLineScannerConfigParser.onError(e, builder)

  import ValueConverters.Implicits._

  val adapterMappingsFile = opt[String](required = false)
  val ontologySqlFile = opt[String](short = 's', required = false)
  val rescanTimeout = opt[Duration](short = 't', required = false)
  val maxTimeToWaitForResults = opt[Duration](name = "max-wait-time", short = 'w', required = false)
  val projectId = opt[String](required = false)
  val credentials = opt[AuthenticationInfo](required = false)
  val url = opt[String](required = false)
  val outputFile = opt[String](short = 'o', required = false)
  val pmUrl = opt[String](short = 'r', required = false)

  def toTypesafeConfig: Config = {
    import scala.collection.JavaConverters._
    import ScannerConfig.ScannerConfigKeys

    def credentialKey(k: String) = s"${ScannerConfigKeys.credentials}.$k"
    def durationKey(k: String)(unitKey: String) = s"$k.$unitKey"

    import net.shrine.utilities.scallop.{Keys => BaseKeys}
    
    val exceptDurations = Map(
      ScannerConfigKeys.adapterMappingsFile -> adapterMappingsFile.get,
      ScannerConfigKeys.ontologySqlFile -> ontologySqlFile.get,
      ScannerConfigKeys.projectId -> projectId.get,
      ScannerConfigKeys.shrineUrl -> url.get,
      ScannerConfigKeys.pmUrl -> pmUrl.get,
      ScannerConfigKeys.outputFile -> outputFile.get,
      credentialKey(BaseKeys.domain) -> credentials.get.map(_.domain),
      credentialKey(BaseKeys.username) -> credentials.get.map(_.username),
      credentialKey(BaseKeys.password) -> credentials.get.map(_.credential.value))

    import CommandLineScannerConfigParser.timeUnitsToNames
      
    val timeoutTupleOption = rescanTimeout.get.map(d => durationKey(ScannerConfigKeys.reScanTimeout)(timeUnitsToNames(d.unit)) -> Some(d.length))
    
    val maxWaitTimeTupleOption = maxTimeToWaitForResults.get.map(d => durationKey(ScannerConfigKeys.maxTimeToWaitForResults)(timeUnitsToNames(d.unit)) -> Some(d.length))
      
    val withDurations = exceptDurations ++ timeoutTupleOption ++ maxWaitTimeTupleOption

    ConfigFactory.parseMap(withDurations.collect { case (k, Some(v)) => (k, v) }.asJava)
  }
}

object CommandLineScannerConfigParser {
  private val timeUnitsToNames = {
    import TimeUnit._
    import net.shrine.utilities.scallop.{Keys => BaseKeys}
    
    Map(MILLISECONDS -> BaseKeys.milliseconds, SECONDS -> BaseKeys.seconds, MINUTES -> BaseKeys.minutes)
  }
  
  private def onError(e: Throwable, scallop: Scallop): String = {
    //NB: Could add logging here, if desired.
    ""
  }
}
