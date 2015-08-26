package net.shrine.config.mappings

import org.junit.Test
import net.shrine.util.ShouldMatchersForJUnit

/**
 * @author clint
 * @date Oct 22, 2013
 */
abstract class AbstractSimpleAdapterMappingsSourceTest extends ShouldMatchersForJUnit {
  protected def sourcesThatShouldFail: Iterable[() => AdapterMappingsSource]

  protected def sourcesThatShouldWork: Iterable[() => AdapterMappingsSource]

  protected def doTestLoad(source: AdapterMappingsSource) {
    val mappings = source.load.get

    mappings should not be (null)

    mappings.version should equal(AdapterMappings.Unknown)
    
    mappings.size should be(2)

    mappings.networkTerms should be(Set("""\\i2b2\i2b2\Demographics\Age\0-9 years old\""", """\\i2b2\i2b2\Demographics\"""))
  }

  @Test
  final def testLoad {
    def doTestLoadbadFileName(source: => AdapterMappingsSource): Unit = {
      source.load.isFailure should be(true)
    }

    sourcesThatShouldFail.foreach(source => doTestLoadbadFileName(source()))

    sourcesThatShouldWork.foreach(source => doTestLoad(source()))
  }
}