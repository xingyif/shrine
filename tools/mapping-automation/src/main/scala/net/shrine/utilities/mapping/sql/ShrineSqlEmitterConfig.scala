package net.shrine.utilities.mapping.sql

import org.rogach.scallop.ScallopConf

/**
 * @author clint
 * @date Jul 16, 2014
 */
final case class ShrineSqlEmitterConfig(inputLocation: InputLocation, filename: String)

object ShrineSqlEmitterConfig {
  import InputLocation._
  
  private final case class CommandLineShrineSqlEmitterArgParser(override val args: Seq[String]) extends ScallopConf(args) {
    val fileSystemCsvFile = opt[String](short = 'f', required = false)
    val classPathCsvFile = opt[String](short = 'c', required = false)
    
    def inputLocation: Option[InputLocation] = {
      if(classPathCsvFile.get.isDefined) { Some(ClassPath) }
      else if(fileSystemCsvFile.get.isDefined) { Some(FileSystem) }
      else { None }
    }
  }
  
  val defaultInputFile = "src/main/csv/shrine.csv"
  
  def fromCommandLineArgs(args: Seq[String]): ShrineSqlEmitterConfig = {
    val parser = CommandLineShrineSqlEmitterArgParser(args)
    
    import parser.{classPathCsvFile, fileSystemCsvFile}
    
    parser.inputLocation match {
      case Some(ClassPath) => ShrineSqlEmitterConfig(ClassPath, classPathCsvFile())
      case Some(FileSystem) => ShrineSqlEmitterConfig(FileSystem, fileSystemCsvFile())
      case _ => ShrineSqlEmitterConfig(FileSystem, defaultInputFile)
    }
  }
}