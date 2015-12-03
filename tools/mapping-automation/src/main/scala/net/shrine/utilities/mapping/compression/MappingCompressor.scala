package net.shrine.utilities.mapping.compression

import net.shrine.config.mappings.AdapterMappings
import java.io.FileReader
import java.io.FileWriter
import scala.xml.XML
import java.io.File
import scala.xml.NodeSeq
import net.shrine.utilities.mapping.commands.SlurpCsv
import net.shrine.utilities.mapping.conversion.commands.ToCsv
import net.shrine.utilities.commands.WriteTo
import net.shrine.utilities.commands.>>>
import net.shrine.util.Versions
import net.shrine.utilities.mapping.InputOutputFileConfig
import net.shrine.utilities.mapping.InputOutputFileArgParser
import net.shrine.utilities.mapping.compression.commands.ToOntTermMap
import net.shrine.utilities.mapping.compression.commands.Compress
import net.shrine.utilities.mapping.generation.commands.ToAdapterMappings

/**
 * @author clint
 * @date Jul 16, 2014
 */
object MappingCompressor {
  
  def toCommand(config: InputOutputFileConfig): (String >>> Unit) = {
    SlurpCsv andThen ToOntTermMap andThen Compress andThen ToAdapterMappings andThen ToCsv andThen WriteTo(config.outputFile)
  }

  def main(args: Array[String]): Unit = {
    //
    //Only run anything if all needed args are present.  If they're not, Scallop
    //will have printed explanatory messages, so we can just quit

    val parser = InputOutputFileArgParser(args)

    val appName = "Shrine adapter mappings compressor"

    if (parser.shouldShowHelp) {
      parser.showHelpAndExit(appName)
    }

    if (parser.shouldShowVersion) {
      parser.showVersionAndExit(appName)
    }

    InputOutputFileConfig.fromCommandLineArgs(parser) match {
      case None => { parser.showHelpAndExit(appName) }
      case Some(config) => {
        val command = toCommand(config)

        command(config.inputFile)
      }
    }
  }
}