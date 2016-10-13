package net.shrine.config.mappings

import java.io.InputStreamReader
import java.io.Reader
import java.net.URL

/**
 * @author clint
 * @date Mar 6, 2012
 */
trait ClasspathAdapterMappingsSource extends ReaderAdapterMappingsSource {
  def mappingFileName: String
  
  //NB: Will blow up loudly if mapping file isn't found
  final override protected def reader: Reader = {
    new InputStreamReader(helpError.openStream())
  }

  final override def lastModified: Long = {
    val conn = helpError.openConnection()
    val lastModified = conn.getLastModified

    // release the resources
    conn.getInputStream.close()

    lastModified
  }

  private def helpError: URL = {
    require(mappingFileName != null)
    val url = getClass.getClassLoader.getResource(mappingFileName)
    require(url != null, s"Couldn't find adapter mapping file '$mappingFileName' on the classpath")
    url
  }
}