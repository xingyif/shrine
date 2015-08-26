package net.shrine.utilities.scanner

import net.shrine.util.ShouldMatchersForJUnit
import org.junit.Test

/**
 * @author clint
 * @date Mar 7, 2013
 */
final class ScanResultsTest extends ShouldMatchersForJUnit {
  @Test
  def testEmpty {
    ScanResults.empty.shouldHaveBeenMapped.isEmpty should be(true)
    ScanResults.empty.shouldNotHaveBeenMapped.isEmpty should be(true)
    ScanResults.empty.neverFinished.isEmpty should be(true)
  }
}