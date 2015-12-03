package net.shrine.config.mappings

import java.io.File
import net.shrine.config.Path

/**
 * @author clint
 * @date Oct 22, 2013
 *
 * A test that validates that FileSystemAdapterMappingsSource can load up a real-world-sized file.
 */
final class FileSystemFormatDetectingAdapterMappingsSourceCanLoadARealFileTest extends AbstractSimpleAdapterMappingsSourceTest {
  override protected def sourcesThatShouldFail = Nil

  private val adapterMappingsXml = Path("src", "test", "resources", "AdapterMappings.xml")
  
  private val pcoriXmlFile = Path("src", "test", "resources", "PCORI_AdapterMappings.xml")
  
  private val pcoriCsvFile = Path("src", "test", "resources", "PCORI_AdapterMappings.csv")
  
  override protected def sourcesThatShouldWork = Seq(
    () => FileSystemFormatDetectingAdapterMappingsSource(adapterMappingsXml),
    () => FileSystemFormatDetectingAdapterMappingsSource(pcoriXmlFile),
    () => FileSystemFormatDetectingAdapterMappingsSource(pcoriCsvFile))

  override protected def doTestLoad(source: AdapterMappingsSource): Unit = {
    val mappings = source.load.get

    mappings should not be (null)

    //TODO: Expedient Hack :(
    //test differently depending on which file we're considering
    source match {
      case FileSystemFormatDetectingAdapterMappingsSource(p) if p == pcoriXmlFile || p == pcoriCsvFile => {
        //TODO: make this less brittle; ideally this test shouldn't fail if Philip adds or removes one entry to the 
        //relevant file.  We could copy that file to src/test/resources, but it's 10s of MB big.  Perhaps that's ok?
        mappings.size should be(314934)
      }
      case FileSystemFormatDetectingAdapterMappingsSource(p) if p == adapterMappingsXml => {
        //TODO: make this less brittle; ideally this test shouldn't fail if Philip adds or removes one entry to the 
        //relevant file.  We could copy that file to src/test/resources, but it's 10s of MB big.  Perhaps that's ok?
        mappings.size should be(15505)

        //Pick an arbitrary key; if one worked, the rest are likely to have worked as well

        val localTerms = mappings.localTermsFor("""\\SHRINE\SHRINE\Diagnoses\Injury and poisoning\Fractures\Other fractures\Other and unspecified fracture\""")

        localTerms should equal(Set(
          """\i2b2\Diagnoses\Injury and poisoning (800-999)\Fracture of neck and trunk (805-809)\(807) Fracture of rib(s), sternum~\(807-5) Closed fracture of larynx~\""",
          """\i2b2\Diagnoses\Injury and poisoning (800-999)\Fracture of neck and trunk (805-809)\(807) Fracture of rib(s), sternum~\(807-2) Closed fracture of sternum\""",
          """\i2b2\Diagnoses\zz V-codes\Misc healthcare circumnstances (V60-V69)\(V66) Convalescence and palliativ~\(V66-4) Convalescence following t~\""",
          """\i2b2\Diagnoses\zz V-codes\Followup care and procedures (V50-V59)\(V54) Other orthopedic aftercare3\(V54-0) Aftercare involving remov~\(V54-01) Encounter for removal of ~\""",
          """\i2b2\Diagnoses\Injury and poisoning (800-999)\Fracture of neck and trunk (805-809)\(807) Fracture of rib(s), sternum~\(807-4) Flail chest\""",
          """\i2b2\Diagnoses\zz V-codes\Misc healthcare circumnstances (V60-V69)\(V67) Follow-up examination\(V67-4) Follow-up examination fol~\""",
          """\i2b2\Diagnoses\Injury and poisoning (800-999)\Fracture of neck and trunk (805-809)\(809) Ill-defined fractures of bo~\(809-0) Fracture of bones of trun~\""",
          """\i2b2\Diagnoses\Injury and poisoning (800-999)\Fracture of neck and trunk (805-809)\(809) Ill-defined fractures of bo~\(809-1) Fracture of bones of trun~\""",
          """\i2b2\Diagnoses\Injury and poisoning (800-999)\Fracture of lower limb (820-829)\(829) Fracture of unspecified bon~\(829-0) Fracture of unspecified b~\""",
          """\i2b2\Diagnoses\Injury and poisoning (800-999)\Fracture of lower limb (820-829)\(829) Fracture of unspecified bon~\(829-1) Fracture of unspecified b~\""",
          """\i2b2\Diagnoses\Injury and poisoning (800-999)\Late effects of external causes (905-909)\(905) Late effects of musculoskel~\(905-5) Late effect of fracture o~\""",
          """\i2b2\Diagnoses\Injury and poisoning (800-999)\Late effects of external causes (905-909)\(905) Late effects of musculoskel~\(905-1) Late effect of fracture o~\""",
          """\i2b2\Diagnoses\Injury and poisoning (800-999)\Fracture of lower limb (820-829)\(828) Multiple fractures involvin~\(828-0) Multiple fractures involv~\""",
          """\i2b2\Diagnoses\Injury and poisoning (800-999)\Fracture of lower limb (820-829)\(828) Multiple fractures involvin~\(828-1) Multiple fractures involv~\""",
          """\i2b2\Diagnoses\Injury and poisoning (800-999)\Fracture of neck and trunk (805-809)\(807) Fracture of rib(s), sternum~\(807-1) Open fracture of rib(s)\(807-18) Open fracture of eight o~\""",
          """\i2b2\Diagnoses\Injury and poisoning (800-999)\Fracture of neck and trunk (805-809)\(807) Fracture of rib(s), sternum~\(807-1) Open fracture of rib(s)\(807-15) Open fracture of five ri~\""",
          """\i2b2\Diagnoses\Injury and poisoning (800-999)\Fracture of neck and trunk (805-809)\(807) Fracture of rib(s), sternum~\(807-1) Open fracture of rib(s)\(807-14) Open fracture of four ri~\""",
          """\i2b2\Diagnoses\Injury and poisoning (800-999)\Fracture of neck and trunk (805-809)\(807) Fracture of rib(s), sternum~\(807-6) Open fracture of larynx a~\""",
          """\i2b2\Diagnoses\Injury and poisoning (800-999)\Fracture of neck and trunk (805-809)\(807) Fracture of rib(s), sternum~\(807-1) Open fracture of rib(s)\(807-19) Open fracture of multipl~\""",
          """\i2b2\Diagnoses\Injury and poisoning (800-999)\Fracture of neck and trunk (805-809)\(807) Fracture of rib(s), sternum~\(807-1) Open fracture of rib(s)\(807-11) Open fracture of one rib\""",
          """\i2b2\Diagnoses\Injury and poisoning (800-999)\Fracture of neck and trunk (805-809)\(807) Fracture of rib(s), sternum~\(807-1) Open fracture of rib(s)\(807-10) Open fracture of rib(s),~\""",
          """\i2b2\Diagnoses\Injury and poisoning (800-999)\Fracture of neck and trunk (805-809)\(807) Fracture of rib(s), sternum~\(807-1) Open fracture of rib(s)\(807-17) Open fracture of seven r~\""",
          """\i2b2\Diagnoses\Injury and poisoning (800-999)\Fracture of neck and trunk (805-809)\(807) Fracture of rib(s), sternum~\(807-1) Open fracture of rib(s)\(807-16) Open fracture of six ribs\""",
          """\i2b2\Diagnoses\Injury and poisoning (800-999)\Fracture of neck and trunk (805-809)\(807) Fracture of rib(s), sternum~\(807-3) Open fracture of sternum\""",
          """\i2b2\Diagnoses\Injury and poisoning (800-999)\Fracture of neck and trunk (805-809)\(807) Fracture of rib(s), sternum~\(807-1) Open fracture of rib(s)\(807-13) Open fracture of three r~\""",
          """\i2b2\Diagnoses\Injury and poisoning (800-999)\Fracture of neck and trunk (805-809)\(807) Fracture of rib(s), sternum~\(807-1) Open fracture of rib(s)\(807-12) Open fracture of two ribs\""",
          """\i2b2\Diagnoses\zz V-codes\Followup care and procedures (V50-V59)\(V54) Other orthopedic aftercare3\(V54-0) Aftercare involving remov~\(V54-09) Other aftercare involving~\"""))
      }
    }
  }
}