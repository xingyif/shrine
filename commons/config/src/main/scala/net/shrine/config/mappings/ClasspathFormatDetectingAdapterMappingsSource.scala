package net.shrine.config.mappings

/**
 * @author clint
 * @date Sep 3, 2014
 */
final case class ClasspathFormatDetectingAdapterMappingsSource(mappingFileName: String)
  extends ClasspathAdapterMappingsSource with FormatDetectingAdapterMappingsSource