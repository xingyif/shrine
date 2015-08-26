package net.shrine.utilities.batchquerier.components

import java.io.File

/**
 * @author clint
 * @date May 29, 2014
 * 
 * Choose names for output files to avoid overwriting existing files.  
 * Given 'foo', if a file with that name exists, try 'foo.1'; from 
 * there, if foo.N exists, try foo.N+1 until a name that doesn't exist 
 * is found. 
 */
object OutputFileChooser {
  private def exists(name: String): Boolean = (new File(name)).exists
  
  def choose(file: File): File = {
    new File(choose(file.getCanonicalPath))
  }
  
  def choose(name: String): String = {
    if(!exists(name)) { name }
    else {
      def toFileName(suffix: Int): String = s"$name.$suffix"
      
      val names = Iterator.from(1).map(toFileName).dropWhile(exists)
      
      names.next()
    }
  }
}