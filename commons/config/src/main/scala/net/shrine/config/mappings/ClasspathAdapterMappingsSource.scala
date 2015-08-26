package net.shrine.config.mappings

import java.io.InputStreamReader
import java.io.Reader

/**
 * @author clint
 * @date Mar 6, 2012
 */
trait ClasspathAdapterMappingsSource extends ReaderAdapterMappingsSource {
  def mappingFileName: String
  
  //NB: Will blow up loudly if mapping file isn't found
  final override protected def reader: Reader = {
    require(mappingFileName != null)
    
    val mappingStream = getClass.getClassLoader.getResourceAsStream(mappingFileName)
    
    require(mappingStream != null, s"Couldn't find adapter mapping file '$mappingFileName' on the classpath")
    
    new InputStreamReader(mappingStream)
  }
}