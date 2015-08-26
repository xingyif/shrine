package net.shrine.utilities.scanner.commands

import net.shrine.util.ShouldMatchersForJUnit
import org.junit.Test
import net.shrine.utilities.scanner.Disposition
import net.shrine.utilities.scanner.csv.CsvRow

/**
 * @author clint
 * @date Mar 25, 2013
 */
final class OutputCsvTest extends ShouldMatchersForJUnit {
  @Test
  def testApply {
    import Disposition.{ShouldHaveBeenMapped, ShouldNotHaveBeenMapped, NeverFinished}
    
    val csvData = Seq(
    				CsvRow(ShouldNotHaveBeenMapped, """\\SHRINE\SHRINE\Diagnoses\Diseases of the respiratory system\Respiratory infections\Pneumonia (except that caused by TB or STD)\Other bacterial pneumonia\Pneumonia due to Klebsiella pneumoniae\"""),
    				CsvRow(ShouldNotHaveBeenMapped, """\\SHRINE\SHRINE\Diagnoses\Diseases of the skin and subcutaneous tissue\Other inflammatory condition of skin\Unspecified erythematous condition\"""),
    				CsvRow(ShouldHaveBeenMapped, """\\SHRINE\SHRINE\Diagnoses\Diseases of the respiratory system\Respiratory infections\Pneumonia (except that caused by TB or STD)\Other bacterial pneumonia\Pneumonia due to other gram-negative bacteria\"""),
    				CsvRow(NeverFinished, """\\SHRINE\SHRINE\Diagnoses\Diseases of the respiratory system\Respiratory failure, insufficiency, arrest (adult)\"""))

    val expected = """"ShouldNotHaveBeenMapped","\\SHRINE\SHRINE\Diagnoses\Diseases of the respiratory system\Respiratory infections\Pneumonia (except that caused by TB or STD)\Other bacterial pneumonia\Pneumonia due to Klebsiella pneumoniae\"
"ShouldNotHaveBeenMapped","\\SHRINE\SHRINE\Diagnoses\Diseases of the skin and subcutaneous tissue\Other inflammatory condition of skin\Unspecified erythematous condition\"
"ShouldHaveBeenMapped","\\SHRINE\SHRINE\Diagnoses\Diseases of the respiratory system\Respiratory infections\Pneumonia (except that caused by TB or STD)\Other bacterial pneumonia\Pneumonia due to other gram-negative bacteria\"
"NeverFinished","\\SHRINE\SHRINE\Diagnoses\Diseases of the respiratory system\Respiratory failure, insufficiency, arrest (adult)\""""
    
    val actual = OutputCsv(csvData).trim 

    actual should equal(expected)
  }
}