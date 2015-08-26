package net.shrine.utilities.mapping.conversion

import net.shrine.config.mappings.AdapterMappings
import java.io.FileReader
import java.io.FileWriter
import scala.xml.XML
import java.io.File
import scala.xml.NodeSeq
import net.shrine.utilities.mapping.commands.SlurpXml
import net.shrine.utilities.mapping.conversion.commands.ToCsv
import net.shrine.utilities.commands.WriteTo
import net.shrine.util.Versions
import net.shrine.utilities.mapping.InputOutputFileConfig
import net.shrine.utilities.mapping.InputOutputFileArgParser

/**
 * @author clint
 * @date Jul 16, 2014
 */
object XmlToCsvConverter {
  import net.shrine.utilities.commands.>>>

  def toCommand(config: InputOutputFileConfig): (String >>> Unit) = {
    SlurpXml andThen ToCsv andThen WriteTo(config.outputFile)
  }

  def main(args: Array[String]): Unit = {
    //
    //Only run anything if all needed args are present.  If they're not, Scallop
    //will have printed explanatory messages, so we can just quit

    val parser = InputOutputFileArgParser(args)

    val appName = "Shrine XML => CSV adapter mappings file converter"

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