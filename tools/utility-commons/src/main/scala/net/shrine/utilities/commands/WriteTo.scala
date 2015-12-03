package net.shrine.utilities.commands

import java.io.FileWriter
import java.io.File

/**
 * @author clint
 * @date Mar 25, 2013
 */
final case class WriteTo(file: File) extends (String >>> Unit) {
  override def apply(dataToBeWritten: String) {
    val writer = new FileWriter(file)
    
    try {
      writer.write(dataToBeWritten)
    } finally {
      writer.close()
    }
  }
  
  override def toString = s"WriteTo($file)"
}

object WriteTo {
  def apply(filename: String): WriteTo = new WriteTo(new File(filename))
}