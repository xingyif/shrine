package net.shrine.config.mappings

/**
 * @author clint
 * @date Sep 3, 2014
 */
final case class FileSystemFormatDetectingAdapterMappingsSource(mappingFileName: String) extends FileSystemAdapterMappingsSource with FormatDetectingAdapterMappingsSource