package net.shrine.utilities.mapping.commands

import net.shrine.util.ShouldMatchersForJUnit
import org.junit.Test
import net.shrine.utilities.mapping.ExpectedTestMappings

/**
 * @author clint
 * @date Jul 18, 2014
 */
final class SlurpCsvTest extends ShouldMatchersForJUnit {
  @Test
  def testApply: Unit = {
    val mappings = SlurpCsv("src/test/resources/adapter-mappings.csv")
    
    mappings.toSeq should equal(ExpectedTestMappings.pairs)
  }
}