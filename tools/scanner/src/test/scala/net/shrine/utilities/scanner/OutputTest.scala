package net.shrine.utilities.scanner

import org.junit.Test
import net.shrine.util.ShouldMatchersForJUnit
import net.shrine.utilities.commands.CompoundCommand
import net.shrine.utilities.commands.WriteTo
import net.shrine.utilities.scanner.commands.OutputCsv
import net.shrine.utilities.scanner.commands.ToCsvData
import java.io.File

/**
 * @author clint
 * @date Mar 25, 2013
 */
final class OutputTest extends ShouldMatchersForJUnit {
  @Test
  def testTo {
    val file = new File("kslajdklasjd")
    
    val command = Output.to(file)
    
    val CompoundCommand(toString, writeToFile) = command
    
    val WriteTo(actualFile: File) = writeToFile.asInstanceOf[WriteTo]
    
    actualFile should equal(file)
    
    val CompoundCommand(toCsvData, outputCsv) = toString
    
    toCsvData should be(ToCsvData)
    
    outputCsv should equal(OutputCsv)
  }
}