package net.shrine.config.mappings

/**
 * @author clint
 * @date Jun 12, 2014
 */
final class ClasspathFormatDetectingAdapterMappingsSourceTest extends AbstractSimpleAdapterMappingsSourceTest {
  override def sourcesThatShouldFail = Seq(
    () => ClasspathFormatDetectingAdapterMappingsSource("askjdklasd"),
    () => ClasspathFormatDetectingAdapterMappingsSource(null))
    
  override def sourcesThatShouldWork = Seq(
    () => ClasspathFormatDetectingAdapterMappingsSource("AdapterMappings_DEM_AGE_0_9.csv"),
    () => ClasspathFormatDetectingAdapterMappingsSource("AdapterMappings_DEM_AGE_0_9.xml"))
}