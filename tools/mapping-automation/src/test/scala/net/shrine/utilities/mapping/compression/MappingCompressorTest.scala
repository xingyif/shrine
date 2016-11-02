package net.shrine.utilities.mapping.compression

import org.junit.Test

import net.shrine.config.mappings.FileSystemFormatDetectingAdapterMappingsSource
import net.shrine.util.ShouldMatchersForJUnit
import net.shrine.utilities.commands.CompoundCommand
import net.shrine.utilities.commands.WriteTo
import net.shrine.utilities.mapping.InputOutputFileConfig
import net.shrine.utilities.mapping.commands.SlurpCsv
import net.shrine.utilities.mapping.compression.commands.Compress
import net.shrine.utilities.mapping.compression.commands.ToOntTermMap
import net.shrine.utilities.mapping.conversion.commands.ToCsv
import net.shrine.utilities.mapping.generation.commands.ToAdapterMappings


/**
 * @author clint
 * @date Aug 1, 2014
 */
final class MappingCompressorTest extends ShouldMatchersForJUnit {
  @Test
  def testToCommand: Unit = {
    val outputFile = "output.csv"
    
    val config = InputOutputFileConfig("input.csv", outputFile)
    
    val command = MappingCompressor.toCommand(config)
    
    command should equal(CompoundCommand(CompoundCommand(CompoundCommand(CompoundCommand(CompoundCommand(SlurpCsv, ToOntTermMap), Compress), ToAdapterMappings), ToCsv), WriteTo(outputFile))) 
  }
  
  @Test
  def testCommand: Unit = {
    val outputFile = "target/output.csv"
    
    val config = InputOutputFileConfig("src/test/resources/compressable-mappings.csv", outputFile)
    
    val command = MappingCompressor.toCommand(config)
    
    command(config.inputFile)
    
    val actual = FileSystemFormatDetectingAdapterMappingsSource(config.outputFile).load(outputFile).get.mappings
    
    actual should equal(Map("""\\some\shrine\term\""" -> Set("""\\x\y\z\""", """\\a\b\c\""")))
  }
}