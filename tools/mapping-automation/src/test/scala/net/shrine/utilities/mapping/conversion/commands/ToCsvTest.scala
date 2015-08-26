package net.shrine.utilities.mapping.conversion.commands

import net.shrine.util.ShouldMatchersForJUnit
import org.junit.Test
import net.shrine.utilities.mapping.ExpectedTestMappings
import scala.io.Source
import java.io.File
import net.shrine.config.mappings.AdapterMappings
import java.io.StringReader

/**
 * @author clint
 * @date Jul 17, 2014
 */
final class ToCsvTest extends ShouldMatchersForJUnit {
  @Test
  def testApply: Unit = {
    val csv = ToCsv(ExpectedTestMappings.mappings)
    
    val unmarshalledMappings = AdapterMappings.fromCsv(new StringReader(csv)).get
    
    unmarshalledMappings should equal(ExpectedTestMappings.mappings)
  }
}