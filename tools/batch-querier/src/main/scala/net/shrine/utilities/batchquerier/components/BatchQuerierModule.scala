package net.shrine.utilities.batchquerier.components

import java.io.File
import net.shrine.log.Loggable
import net.shrine.utilities.batchquerier.ShrineBatchQuerier
import net.shrine.utilities.batchquerier.commands.FormatForOutput
import net.shrine.utilities.batchquerier.commands.QueryWith
import net.shrine.utilities.batchquerier.commands.ReadXmlQueryDefs
import net.shrine.utilities.batchquerier.commands.ToCsv
import net.shrine.utilities.commands.>>>
import net.shrine.utilities.commands.WriteTo
import net.shrine.utilities.batchquerier.commands.GroupRepeated
import net.shrine.protocol.query.QueryDefinition
import net.shrine.protocol.query.Term
import net.shrine.util.Versions

/**
 * @author clint
 * @date Sep 6, 2013
 */
final class BatchQuerierModule(override val args: Seq[String]) extends ShrineBatchQuerier with BatchQuerierConfigComponent with JerseyShrineClientComponent with TrustsAllCerts  {
  def query: File >>> Unit = {
    val outputFile = OutputFileChooser.choose(config.outputFile)

    ReadXmlQueryDefs andThen
      QueryWith(this, config.queriesPerTerm) andThen
      GroupRepeated andThen
      FormatForOutput andThen
      ToCsv andThen
      WriteTo(outputFile)
  }
}

object BatchQuerierModule {
  def main(args: Array[String]) {

    val batchQuerier = new BatchQuerierModule(args)
    
    val appName = "Shrine Batch Querier"
    
    def printUsageAndExit() {
      println("Usage: ./batch-querier <args>")
      
      batchQuerier.commandLineArgs.showHelpAndExit(appName)
    }
    
    val shouldShowHelp = batchQuerier.commandLineArgs.shouldShowHelp
    
    if(shouldShowHelp) {
      printUsageAndExit()
    }
    
    val shouldShowVersionInfo = batchQuerier.commandLineArgs.shouldShowVersion
    
    if(shouldShowVersionInfo) {
      batchQuerier.commandLineArgs.showVersionAndExit(appName)
    }
    
    batchQuerier.configProvided.left.foreach { message =>
      System.err.println(s"Error: $message")
      
      printUsageAndExit()
    }
    
    val expressionFile = batchQuerier.config.expressionFile

    val command = batchQuerier.query

    command(expressionFile)
  }
}