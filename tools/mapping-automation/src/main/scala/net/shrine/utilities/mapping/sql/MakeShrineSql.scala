package net.shrine.utilities.mapping.sql

import java.io.Reader

/**
 * @author clint
 * @date Jun 13, 2014
 */
object MakeShrineSql {
  def main(args: Array[String]): Unit = {
    val config = ShrineSqlEmitterConfig.fromCommandLineArgs(args)
    
    val fileName = config.filename
    
    import InputLocation._
    
    val reader: Reader = config.inputLocation match {
      case FileSystem => ReadCsv.fromFile(fileName)
      case ClassPath => ReadCsv.fromClasspath(fileName)
      case _ => ReadCsv.fromStdIn
    }
    
    val sqlLines = ShrineSqlEmitter("SHRINE").toSql(ReadCsv(reader))
    
    //Write to std out
    sqlLines.map(_ + "\n").foreach(println)
  }
}