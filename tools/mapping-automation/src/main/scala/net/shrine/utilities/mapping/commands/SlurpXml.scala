package net.shrine.utilities.mapping.commands

import net.shrine.utilities.commands.>>>
import net.shrine.config.mappings.AdapterMappings
import scala.xml.XML
import java.io.File

/**
 * @author clint
 * @date Jul 16, 2014
 */
object SlurpXml extends (String >>> AdapterMappings) {
  override def apply(filename: String): AdapterMappings = {
    val xml = XML.loadFile(new File(filename))
    
    AdapterMappings.fromXml(xml).get
  }
  
  override def toString = "SlurpXml"
}