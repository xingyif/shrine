package net.shrine.config.mappings

import java.io.Reader

/**
 * @author clint
 * @date Jun 12, 2014
 */
trait ReaderAdapterMappingsSource extends AdapterMappingsSource {
  def mappingFileName: String
  
  protected def reader: Reader
}