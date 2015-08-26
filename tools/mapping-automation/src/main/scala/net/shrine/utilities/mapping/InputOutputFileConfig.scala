package net.shrine.utilities.mapping

/**
 * @author clint
 * @date Jul 16, 2014
 */
final case class InputOutputFileConfig(inputFile: String, outputFile: String)

object InputOutputFileConfig {
  def fromCommandLineArgs(argParser: InputOutputFileArgParser): Option[InputOutputFileConfig] = {
    for {
      inputFile <- argParser.inputFile.get
      outputFile <- argParser.outputFile.get
    } yield {
      InputOutputFileConfig(inputFile, outputFile)
    }
  }
}