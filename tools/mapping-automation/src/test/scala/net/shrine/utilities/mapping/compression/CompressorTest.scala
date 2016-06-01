package net.shrine.utilities.mapping.compression

import java.io.FileReader

import org.junit.Test

import net.shrine.config.mappings.AdapterMappings
import net.shrine.ont.OntTerm
import net.shrine.util.ShouldMatchersForJUnit

/**
 * @author clint
 * @date Aug 1, 2014
 */
final class CompressorTest extends ShouldMatchersForJUnit {
  @Test
  def testCompress: Unit = {
    Compressor.compress(Map.empty) should equal(Map.empty)

    {
      val terms: Map[OntTerm, Set[OntTerm]] = {
        Map(OntTerm("""some\shrine\term""") -> Set(
          OntTerm("""x\y\z"""),
          OntTerm("""a\b\c"""),
          OntTerm("""a\b\c\d"""),
          OntTerm("""a\b\c\d\e"""),
          OntTerm("""a\b\c\x""")))
      }

      val expected = Map(OntTerm("""some\shrine\term""") -> Set(
        OntTerm("""x\y\z"""),
        OntTerm("""a\b\c""")))

      Compressor.compress(terms) should equal(expected)
    }

    {
      val terms: Map[OntTerm, Set[OntTerm]] = {
        Map(OntTerm("""some\shrine\term""") -> Set(
          OntTerm("""x\y\z"""),
          OntTerm("""a\b\c""")))
      }

      Compressor.compress(terms) should equal(terms)
    }
  }

  @Test
  def testCompressRealWorldData: Unit = {
    val terms: Map[OntTerm, Set[OntTerm]] = {
      Map(OntTerm("""\\SHRINE\SHRINE\Diagnoses\CERTAIN CONDITIONS ORIGINATING IN THE PERINATAL PERIOD (760-779.99)\MATERNAL CAUSES OF PERINATAL MORBIDITY AND MORTALITY (760-763.99)\Fetus or newborn affected by complications of placenta, cord, and membranes (762)\""") -> Set(
        OntTerm("""\\i2b2\RPDR\Diagnoses\Conditions in the perinatal period (760-779)\Maternally caused (760-763)\(762) Fetus or newborn affected b~\"""),
        OntTerm("""\\i2b2\RPDR\Diagnoses\Conditions in the perinatal period (760-779)\Maternally caused (760-763)\(762) Fetus or newborn affected b~\(762-7) Chorioamnionitis affectin~\"""),
        OntTerm("""\\i2b2\RPDR\Diagnoses\Conditions in the perinatal period (760-779)\Maternally caused (760-763)\(762) Fetus or newborn affected b~\(762-5) Other compression of umbi~\"""),
        OntTerm("""\\i2b2\RPDR\Diagnoses\Conditions in the perinatal period (760-779)\Maternally caused (760-763)\(762) Fetus or newborn affected b~\(762-9) Unspecified abnormality o~\"""),
        OntTerm("""\\i2b2\RPDR\Diagnoses\Conditions in the perinatal period (760-779)\Maternally caused (760-763)\(762) Fetus or newborn affected b~\(762-4) Prolapsed cord affecting ~\"""),
        OntTerm("""\\i2b2\RPDR\Diagnoses\Conditions in the perinatal period (760-779)\Maternally caused (760-763)\(762) Fetus or newborn affected b~\(762-6) Other and unspecified con~\"""),
        OntTerm("""\\i2b2\RPDR\Diagnoses\Conditions in the perinatal period (760-779)\Maternally caused (760-763)\(762) Fetus or newborn affected b~\(762-8) Other specified abnormali~\"""),
        OntTerm("""\\i2b2\RPDR\Diagnoses\Conditions in the perinatal period (760-779)\Maternally caused (760-763)\(762) Fetus or newborn affected b~\(762-3) Placental transfusion syn~\"""),
        OntTerm("""\\i2b2\RPDR\Diagnoses\Conditions in the perinatal period (760-779)\Maternally caused (760-763)\(762) Fetus or newborn affected b~\(762-2) Other and unspecified mor~\"""),
        OntTerm("""\\i2b2\RPDR\Diagnoses\Conditions in the perinatal period (760-779)\Maternally caused (760-763)\(762) Fetus or newborn affected b~\(762-0) Placenta previa affecting~\"""),
        OntTerm("""\\i2b2\RPDR\Diagnoses\Conditions in the perinatal period (760-779)\Maternally caused (760-763)\(762) Fetus or newborn affected b~\(762-1) Other forms of placental ~\""")))
    }
    
    val compressed = Compressor.compress(terms)
    
    compressed should equal(Map(OntTerm("""\\SHRINE\SHRINE\Diagnoses\CERTAIN CONDITIONS ORIGINATING IN THE PERINATAL PERIOD (760-779.99)\MATERNAL CAUSES OF PERINATAL MORBIDITY AND MORTALITY (760-763.99)\Fetus or newborn affected by complications of placenta, cord, and membranes (762)\""") -> Set(
        OntTerm("""\\i2b2\RPDR\Diagnoses\Conditions in the perinatal period (760-779)\Maternally caused (760-763)\(762) Fetus or newborn affected b~\"""))))
  }

  @Test
  def testCompressRealWorldFile: Unit = {
    val mappings = {

      val fileName = "src/test/resources/compressable-real-world-mappings.csv"
      AdapterMappings.fromCsv(fileName,new FileReader(fileName)).get.mappings.map {
        case (shrineTerm, i2b2Terms) =>
          OntTerm(shrineTerm) -> i2b2Terms.map(OntTerm(_))
      }
    }
    //Sanity-check one term:
    val shrineTermWeCareAbout = OntTerm("""\\SHRINE\SHRINE\Diagnoses\CERTAIN CONDITIONS ORIGINATING IN THE PERINATAL PERIOD (760-779.99)\MATERNAL CAUSES OF PERINATAL MORBIDITY AND MORTALITY (760-763.99)\Fetus or newborn affected by complications of placenta, cord, and membranes (762)\""")
    
    mappings.get(shrineTermWeCareAbout).get.size should equal(11)

    val compressed = Compressor.compress(mappings)

    compressed should not equal (mappings)
    
    val localTerms = compressed.get(shrineTermWeCareAbout).get

    localTerms should equal(Set(OntTerm("""\\i2b2\RPDR\Diagnoses\Conditions in the perinatal period (760-779)\Maternally caused (760-763)\(762) Fetus or newborn affected b~\""")))
  }
}