package net.shrine.config.mappings

import scala.util.Try
import scala.xml.XML

/**
 * @author clint
 * @date Jun 12, 2014
 */
trait XmlAdapterMappingsSource extends ReaderAdapterMappingsSource {
  final override def load: Try[AdapterMappings] = AdapterMappings.fromXml(XML.load(reader))
}