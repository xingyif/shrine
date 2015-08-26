package net.shrine.config.mappings

import net.shrine.util.ShouldMatchersForJUnit
import org.junit.Test
import java.io.InputStreamReader
import java.io.Reader
import java.io.InputStream

/**
 * @author clint
 * @date Jul 17, 2014
 */
final class CsvTest extends ShouldMatchersForJUnit {
  @Test
  def testSlurp: Unit = doTestSlurp(Csv.slurp(_).iterator)
  
  @Test
  def testLazySlurp: Unit = doTestSlurp(Csv.lazySlurp)
  
  private def doTestSlurp(iter: Reader => Iterator[(String, String)]): Unit = {
    final class MockReader(stream: InputStream) extends InputStreamReader(stream) {
      var isClosed = false
      
      override def close(): Unit = {
        super.close()
        
        isClosed = true
      } 
    }
    
    val reader = new MockReader(getClass.getClassLoader.getResourceAsStream("AdapterMappings_DEM_AGE_0_9.csv"))

    reader.isClosed should be(false)
    
    val lines = iter(reader).toSeq
    
    lines.size should be(11)
    
    reader.isClosed should be(true)
    
    lines should equal(Seq(
      ("""\\i2b2\i2b2\Demographics\Age\0-9 years old\""", """\\i2b2\LOCAL\DEM|AGE:0"""),
      ("""\\i2b2\i2b2\Demographics\Age\0-9 years old\""", """\\i2b2\LOCAL\DEM|AGE:1"""),
      ("""\\i2b2\i2b2\Demographics\Age\0-9 years old\""", """\\i2b2\LOCAL\DEM|AGE:2"""),
      ("""\\i2b2\i2b2\Demographics\Age\0-9 years old\""", """\\i2b2\LOCAL\DEM|AGE:3"""),
      ("""\\i2b2\i2b2\Demographics\Age\0-9 years old\""", """\\i2b2\LOCAL\DEM|AGE:4"""),
      ("""\\i2b2\i2b2\Demographics\Age\0-9 years old\""", """\\i2b2\LOCAL\DEM|AGE:5"""),
      ("""\\i2b2\i2b2\Demographics\Age\0-9 years old\""", """\\i2b2\LOCAL\DEM|AGE:6"""),
      ("""\\i2b2\i2b2\Demographics\Age\0-9 years old\""", """\\i2b2\LOCAL\DEM|AGE:7"""),
      ("""\\i2b2\i2b2\Demographics\Age\0-9 years old\""", """\\i2b2\LOCAL\DEM|AGE:8"""),
      ("""\\i2b2\i2b2\Demographics\Age\0-9 years old\""", """\\i2b2\LOCAL\DEM|AGE:9"""),
      ("""\\i2b2\i2b2\Demographics\""", """\\i2b2\LOCAL\DEM""")))
  }
}