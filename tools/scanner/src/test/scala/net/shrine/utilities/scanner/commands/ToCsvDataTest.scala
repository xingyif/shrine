package net.shrine.utilities.scanner.commands

import net.shrine.util.ShouldMatchersForJUnit
import org.junit.Test
import net.shrine.utilities.scanner.csv.CsvRow
import net.shrine.utilities.scanner.Disposition
import net.shrine.utilities.scanner.ScanResults

/**
 * @author clint
 * @date Mar 25, 2013
 */
final class ToCsvDataTest extends ShouldMatchersForJUnit {
  @Test
  def testApply {
    import Disposition.{ ShouldHaveBeenMapped, ShouldNotHaveBeenMapped, NeverFinished, Failed }

    val shouldHaveBeenMapped = Set("""\\SHRINE\SHRINE\Diagnoses\Diseases of the respiratory system\Respiratory infections\Pneumonia (except that caused by TB or STD)\Other bacterial pneumonia\Pneumonia due to Klebsiella pneumoniae\""")
    val shouldNOTHaveBeenMapped = Set("""\\SHRINE\SHRINE\Diagnoses\Diseases of the respiratory system\Respiratory infections\Pneumonia (except that caused by TB or STD)\Other bacterial pneumonia\Pneumonia due to Klebsiella pneumoniae\""", """\\SHRINE\SHRINE\Diagnoses\Diseases of the skin and subcutaneous tissue\Other inflammatory condition of skin\Unspecified erythematous condition\""")
    val neverFinished = Set("""\\SHRINE\SHRINE\Diagnoses\Diseases of the respiratory system\Respiratory failure, insufficiency, arrest (adult)\""")
    val failed = Set("""\\SHRINE\SHRINE\Foo""")

    val scanResults = ScanResults(shouldHaveBeenMapped, shouldNOTHaveBeenMapped, neverFinished, failed)

    val expected = Seq(
      CsvRow(ShouldHaveBeenMapped, """\\SHRINE\SHRINE\Diagnoses\Diseases of the respiratory system\Respiratory infections\Pneumonia (except that caused by TB or STD)\Other bacterial pneumonia\Pneumonia due to other gram-negative bacteria\"""),
      CsvRow(ShouldNotHaveBeenMapped, """\\SHRINE\SHRINE\Diagnoses\Diseases of the respiratory system\Respiratory infections\Pneumonia (except that caused by TB or STD)\Other bacterial pneumonia\Pneumonia due to Klebsiella pneumoniae\"""),
      CsvRow(ShouldNotHaveBeenMapped, """\\SHRINE\SHRINE\Diagnoses\Diseases of the skin and subcutaneous tissue\Other inflammatory condition of skin\Unspecified erythematous condition\"""),
      CsvRow(NeverFinished, """\\SHRINE\SHRINE\Diagnoses\Diseases of the respiratory system\Respiratory failure, insufficiency, arrest (adult)\"""),
      CsvRow(Failed, """\\SHRINE\SHRINE\Foo""")
      )

    val actual = ToCsvData(scanResults)

    def termsWith(disposition: Disposition): PartialFunction[CsvRow, String] = {
      case CsvRow(d, term) if d == disposition => term
    }
    
    actual.collect(termsWith(ShouldHaveBeenMapped)).toSet should equal(shouldHaveBeenMapped)
    actual.collect(termsWith(ShouldNotHaveBeenMapped)).toSet should equal(shouldNOTHaveBeenMapped)
    actual.collect(termsWith(NeverFinished)).toSet should equal(neverFinished)
    actual.collect(termsWith(Failed)).toSet should equal(failed)
  }
}