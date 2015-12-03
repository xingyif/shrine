package net.shrine.utilities.mapping.commands

import net.shrine.utilities.commands.>>>
import net.shrine.config.mappings.Csv
import java.io.FileReader

/**
 * @author clint
 * @date Jul 16, 2014
 */
object SlurpCsv extends (String >>> Iterator[(String, String)]) {
  override def apply(fileName: String): Iterator[(String, String)] = {
    Csv.lazySlurp(new FileReader(fileName))
  } 
  
  override def toString = "SlurpCsv"
}