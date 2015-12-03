package net.shrine.protocol

import net.shrine.util.ShouldMatchersForJUnit
import org.junit.Test

/**
 * @author clint
 * @date Mar 3, 2014
 */
final class TimeoutTest extends ShouldMatchersForJUnit {
  @Test
  def testXmlRoundTrip {
    val timeout = Timeout(NodeId("X"))
    
    val unmarshalled = Timeout.fromXml(DefaultBreakdownResultOutputTypes.toSet)(timeout.toXml).get
    
    unmarshalled should equal(timeout)
  }
}