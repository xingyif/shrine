package net.shrine.util

import org.junit.Test

/**
 * @author clint
 * @date Nov 26, 2014
 */
final class StringEnrichmentsTest extends ShouldMatchersForJUnit {
  @Test
  def testTryToXml: Unit = {
    import StringEnrichments._
    
    (null: String).tryToXml.isFailure should be(true)
    "".tryToXml.isFailure should be(true)
    "asl;kda;sd".tryToXml.isFailure should be(true)
    
    "<foo/>".tryToXml.get should equal(<foo/>)
  }
}