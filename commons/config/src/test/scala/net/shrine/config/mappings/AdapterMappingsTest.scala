package net.shrine.config.mappings

import org.junit.Test
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.XmlRootElement
import net.shrine.util.ShouldMatchersForJUnit
import java.io.StringReader
import scala.util.Try
import java.io.FileReader

/**
 * @author Andrew McMurry, MS
 * @author clint 
 * @date Jan 6, 2010
 * @link http://cbmi.med.harvard.edu
 * @link http://chip.org
 *       <p/>
 *       NOTICE: This software comes with NO guarantees whatsoever and is
 *       licensed as Lgpl Open Source
 * @link http://www.gnu.org/licenses/lgpl.html
 */
object AdapterMappingsTest {
  private val CORE_KEY_DEMOGRAPHICS_0_9 = """\\i2b2\i2b2\Demographics\Age\0-9 years old\"""
  private val CORE_KEY_TEST = """\\i2b2\i2b2\TEST\KEY\"""
  private val CORE_KEY_INVALID = """THIS IS NOT A VALID GLOBAL KEY"""
  private val LOCAL_KEY_DEMOGRAPHICS_AGE_4 = """\\i2b2\LOCAL\DEM|AGE:4"""
  private val LOCAL_KEY_DEMOGRAPHICS_AGE_TEST = """\\i2b2\LOCAL\DEM|AGE:TEST"""
}

final class AdapterMappingsTest extends ShouldMatchersForJUnit {
  import AdapterMappingsTest._

  private val mappings = (ClasspathFormatDetectingAdapterMappingsSource("AdapterMappings_DEM_AGE_0_9.xml")).load.get

  @Test
  def testDefaultConstructor {
    val mappings = new AdapterMappings
    
    mappings.mappings should equal(Map.empty)
    mappings.version should equal(AdapterMappings.Unknown)
  }
  
  @Test
  def testGetMappings {
    mappings.localTermsFor(CORE_KEY_INVALID) should not be (null)
    mappings.localTermsFor(CORE_KEY_INVALID).size should equal(0)

    mappings.localTermsFor(CORE_KEY_DEMOGRAPHICS_0_9) should not be (null)
    mappings.localTermsFor(CORE_KEY_DEMOGRAPHICS_0_9).size should equal(10)
  }

  @Test
  def testAddMapping {
    (mappings + (CORE_KEY_DEMOGRAPHICS_0_9 -> LOCAL_KEY_DEMOGRAPHICS_AGE_4)).localTermsFor(CORE_KEY_DEMOGRAPHICS_0_9).size should equal(10)
    
    ((mappings + (CORE_KEY_DEMOGRAPHICS_0_9 -> LOCAL_KEY_DEMOGRAPHICS_AGE_4)) eq mappings) should be(true)

    (mappings + (CORE_KEY_DEMOGRAPHICS_0_9 -> LOCAL_KEY_DEMOGRAPHICS_AGE_TEST)).localTermsFor(CORE_KEY_DEMOGRAPHICS_0_9).size should equal(11)

    mappings.localTermsFor(CORE_KEY_TEST).size should equal(0)
    
    (mappings + (CORE_KEY_TEST -> LOCAL_KEY_DEMOGRAPHICS_AGE_TEST)).localTermsFor(CORE_KEY_TEST).size should equal(1)
  }

  @Test
  def testSerializeXml: Unit = doTestSerialize(_.toXml, AdapterMappings.fromXml)
  
  @Test
  def testSerializeCsv: Unit = doTestSerialize(_.toCsv, (s: String) => AdapterMappings.fromCsv(new StringReader(s)))
  
  private def doTestSerialize[S](serialize: AdapterMappings => S, deserialize: S => Try[AdapterMappings]): Unit = {
    val m = AdapterMappings.empty  ++ Seq("core1" -> "local1", 
							    	   "core1" -> "local2",
							    	   "core2" -> "local1",
							    	   "core2" -> "local2",
							    	   "core2" -> "local3")
							    	   
    val unmarshalled = deserialize(serialize(m)).get
    
    unmarshalled should equal(m)
  }
  
  @Test
  def testMultiFormatRoundTrip: Unit = {
    val m = AdapterMappings.empty  ++ Seq(
                       "core1" -> "local1", 
							    	   "core1" -> "local2",
							    	   "core2" -> "local1",
							    	   "core2" -> "local2",
							    	   "core2" -> "local3")
							    	   
    val unmarshalled = AdapterMappings.fromCsv(new StringReader(AdapterMappings.fromXml(m.toXml).get.toCsv)).get
    
    unmarshalled should equal(m)
  }
  
  @Test
  def testEmpty {
    AdapterMappings.empty.size should equal(0)
  }
  
  @Test 
  def testVersionParsing {
    val expected = "1.2.3-foo"
      
    val mappings = (ClasspathFormatDetectingAdapterMappingsSource("AdapterMappingsWithVersion.xml")).load.get
    
    mappings.version should equal(expected)
    
    //Check that mapping parsing worked as normal
    mappings.localTermsFor(CORE_KEY_INVALID) should not be (null)
    mappings.localTermsFor(CORE_KEY_INVALID).size should equal(0)

    mappings.localTermsFor(CORE_KEY_DEMOGRAPHICS_0_9) should not be (null)
    mappings.localTermsFor(CORE_KEY_DEMOGRAPHICS_0_9).size should equal(10)
  } 
  
  @Test
  def testFromCsv: Unit = {
    val mappings = AdapterMappings.fromCsv(new FileReader("src/test/resources/simple-mappings.csv")).get
    
    mappings.mappings should equal(Map(
        """\\X\Y\Z\A\""" -> Set("""\\A\B\C\A1\""","""\\A\B\C\A2\"""),
        """\\X\Y\Z\""" -> Set("""\\A\B\C\""")))
  }
}