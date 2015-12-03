package net.shrine.config.mappings

import net.shrine.config.Path

/**
 * @author clint
 * @date Jun 12, 2014
 */
final class FileSystemFormatDetectingAdapterMappingsSourceTest extends AbstractSimpleAdapterMappingsSourceTest {
  override def sourcesThatShouldFail = Seq(
    () => FileSystemFormatDetectingAdapterMappingsSource("mksaldklasjdklasjd"),
    () => FileSystemFormatDetectingAdapterMappingsSource(null: String))
    
  override def sourcesThatShouldWork = Seq(
    () => FileSystemFormatDetectingAdapterMappingsSource(Path("src", "test", "resources", "AdapterMappings_DEM_AGE_0_9.csv")),
    () => FileSystemFormatDetectingAdapterMappingsSource(Path("src", "test", "resources", "AdapterMappings_DEM_AGE_0_9.xml"))
    )
}