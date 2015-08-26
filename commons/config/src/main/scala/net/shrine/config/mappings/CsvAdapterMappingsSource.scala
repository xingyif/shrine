package net.shrine.config.mappings

import scala.util.Try

/**
 * @author clint
 * @date Jun 12, 2014
 */
trait CsvAdapterMappingsSource extends ReaderAdapterMappingsSource {
  final override def load: Try[AdapterMappings] = AdapterMappings.fromCsv(reader)
}