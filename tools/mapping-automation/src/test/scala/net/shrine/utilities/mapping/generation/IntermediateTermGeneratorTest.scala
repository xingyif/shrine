package net.shrine.utilities.mapping.generation

import net.shrine.util.ShouldMatchersForJUnit
import org.junit.Test
import net.shrine.utilities.commands.CompoundCommand
import net.shrine.utilities.mapping.commands.SlurpCsv
import net.shrine.utilities.mapping.generation.commands.Generate
import net.shrine.utilities.mapping.generation.commands.ToAdapterMappings
import net.shrine.utilities.mapping.conversion.commands.ToCsv
import net.shrine.utilities.commands.WriteTo
import net.shrine.config.mappings.AdapterMappings
import java.io.FileReader

/**
 * @author clint
 * @since Jul 18, 2014
 */
final class IntermediateTermGeneratorTest extends ShouldMatchersForJUnit {
  @Test
  def testToCommand: Unit = {
    val outputFile = "bar.csv"

    val config = IntermediateTermGeneratorConfig("foo.csv", outputFile, Some(2))

    val command = IntermediateTermGenerator.toCommand(config)

    command should equal(CompoundCommand(CompoundCommand(CompoundCommand(CompoundCommand(SlurpCsv, Generate(Some(2))), ToAdapterMappings), ToCsv), WriteTo(outputFile)))
  }

  @Test
  def testCommand: Unit = {
    val inputFile = "src/test/resources/simple-mappings.csv"

    val outputFile = "target/bar.csv"

    val config = IntermediateTermGeneratorConfig(inputFile, outputFile)

    val command = IntermediateTermGenerator.toCommand(config)

    command(inputFile)

    val generatedMappings = AdapterMappings.fromCsv(outputFile,new FileReader(outputFile)).get

    val xyza = """\\X\Y\Z\A\"""
    val xyz = """\\X\Y\Z\"""
    val xy = """\\X\Y\"""
    val x = """\\X\"""

    val abc = """\\A\B\C\"""
    val abca1 = """\\A\B\C\A1\"""
    val abca2 = """\\A\B\C\A2\"""

    generatedMappings should equal(
      AdapterMappings(
        outputFile,
        AdapterMappings.Unknown,
        Map(
          xyza -> Set(abca1, abca2),
          xyz -> Set(abca1, abca2, abc),
          xy -> Set(abca1, abca2, abc),
          x -> Set(abca1, abca2, abc)
        )
      )
    )
  }
  
  @Test
  def testCommandHLevelRestriction: Unit = {
    val inputFile = "src/test/resources/simple-mappings.csv"

    val outputFile = "target/bar.csv"

    val config = IntermediateTermGeneratorConfig(inputFile, outputFile, Some(1))

    val command = IntermediateTermGenerator.toCommand(config)

    command(inputFile)

    val generatedMappings = AdapterMappings.fromCsv(outputFile,new FileReader(outputFile)).get

    val xyza = """\\X\Y\Z\A\"""
    val xyz = """\\X\Y\Z\"""
    val xy = """\\X\Y\"""
    val x = """\\X\"""

    val abc = """\\A\B\C\"""
    val abca1 = """\\A\B\C\A1\"""
    val abca2 = """\\A\B\C\A2\"""

    generatedMappings should equal(AdapterMappings(outputFile,AdapterMappings.Unknown, Map(
      xyza -> Set(abca1, abca2),
      xyz -> Set(abca1, abca2, abc),
      xy -> Set(abca1, abca2, abc))))
  }
}