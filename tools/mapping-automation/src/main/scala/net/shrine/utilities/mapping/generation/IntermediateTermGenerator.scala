package net.shrine.utilities.mapping.generation

import net.shrine.utilities.commands.>>>
import net.shrine.utilities.mapping.commands.SlurpCsv
import net.shrine.utilities.mapping.generation.commands.ToAdapterMappings
import net.shrine.utilities.mapping.conversion.commands.ToCsv
import net.shrine.utilities.mapping.generation.commands.Generate
import net.shrine.utilities.commands.WriteTo
import java.io.File

/**
 * @author clint
 * @date Jul 17, 2014
 */
object IntermediateTermGenerator {
  def toCommand(config: IntermediateTermGeneratorConfig): (String >>> Unit) = {
    SlurpCsv andThen Generate(config.hLevelToStopAt) andThen ToAdapterMappings andThen ToCsv andThen WriteTo(config.outputFile)
  }

  def main(args: Array[String]): Unit = {
    val parser = IntermediateTermGeneratorArgParser(args)

    val appName = "Shrine Intermediate-Mappings Generator"
    
    if (parser.shouldShowHelp) {
      parser.showHelpAndExit(appName)
    }

    if (parser.shouldShowVersion) {
      parser.showVersionAndExit(appName)
    }
    
    IntermediateTermGeneratorConfig.fromCommandLineArgs(parser) match {
      case None => { parser.showHelpAndExit(appName) }
      case Some(config) => {
        val command = toCommand(config)

        command(config.inputFile)
      }
    }
  }
} 
