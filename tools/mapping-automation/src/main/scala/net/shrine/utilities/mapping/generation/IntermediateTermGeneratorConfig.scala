package net.shrine.utilities.mapping.generation

import org.rogach.scallop.ScallopConf
import net.shrine.utilities.scallop.AbstractArgParser

/**
 * @author clint
 * @date Jul 16, 2014
 */
final case class IntermediateTermGeneratorConfig(inputFile: String, outputFile: String, hLevelToStopAt: Option[Int] = None)

object IntermediateTermGeneratorConfig {
  def fromCommandLineArgs(parser: IntermediateTermGeneratorArgParser): Option[IntermediateTermGeneratorConfig] = {
    for {
      inputFile <- parser.inputFile.get
      outputFile <- parser.outputFile.get
      maxHLevel = parser.minHLevel.get
    } yield {
      IntermediateTermGeneratorConfig(inputFile, outputFile, maxHLevel)
    }
  } 
}