package net.shrine.config.mappings

import java.io.File
import java.io.FileReader
import java.io.Reader

/**
 * @author clint
 * @date Mar 26, 2013
 */
trait FileSystemAdapterMappingsSource extends ReaderAdapterMappingsSource {
  final def mappingFile: File = new File(mappingFileName)

  final override def lastModified: Long = {
    requireHelp
    mappingFile.lastModified
  }

  //NB: Will blow up loudly if mapping file isn't found
  final override protected def reader: Reader = {
    requireHelp
    new FileReader(mappingFile)
  }

  private final def requireHelp = {
    require(mappingFile != null)
    require(mappingFile.exists, s"Couldn't find adapter mapping file '${ mappingFile.getCanonicalPath }'")
  }
}