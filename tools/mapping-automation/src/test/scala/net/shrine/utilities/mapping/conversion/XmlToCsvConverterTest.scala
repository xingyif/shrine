package net.shrine.utilities.mapping.conversion

import java.io.StringReader

import org.junit.Test

import net.shrine.config.mappings.AdapterMappings
import net.shrine.util.ShouldMatchersForJUnit
import net.shrine.utilities.commands.CompoundCommand
import net.shrine.utilities.commands.WriteTo
import net.shrine.utilities.mapping.ExpectedTestMappings
import net.shrine.utilities.mapping.InputOutputFileConfig
import net.shrine.utilities.mapping.commands.SlurpXml
import net.shrine.utilities.mapping.conversion.commands.ToCsv

/**
 * @author clint
 * @since Jul 17, 2014
 */
final class XmlToCsvConverterTest extends ShouldMatchersForJUnit {
  @Test
  def testToCommand: Unit = {
    val outputFile = "output.csv"
    
    val config = InputOutputFileConfig("input.csv", outputFile)
    
    val command = XmlToCsvConverter.toCommand(config)
    
    command should equal(CompoundCommand(CompoundCommand(SlurpXml, ToCsv), WriteTo(outputFile))) 
  }
  
  @Test
  def testCommand: Unit = {
    val command = SlurpXml andThen ToCsv
    val fileName = "src/test/resources/AdapterMappings.xml"
    val csv = command(fileName)
    
    val unmarshalled = AdapterMappings.fromCsv(fileName,new StringReader(csv)).get
    
    unmarshalled should equal(ExpectedTestMappings.mappings.copy(source = "src/test/resources/AdapterMappings.xml"))
  }
}