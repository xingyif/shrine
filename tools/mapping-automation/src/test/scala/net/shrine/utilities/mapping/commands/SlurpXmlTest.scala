package net.shrine.utilities.mapping.commands

import net.shrine.util.ShouldMatchersForJUnit
import org.junit.Test
import net.shrine.utilities.mapping.ExpectedTestMappings

/**
 * @author clint
 * @date Jul 17, 2014
 */
final class SlurpXmlTest extends ShouldMatchersForJUnit {
  @Test
  def testApply: Unit = {
    val filename = "src/test/resources/AdapterMappings.xml"
    SlurpXml(filename) should equal(ExpectedTestMappings.mappings.copy(source = filename))
  }
}