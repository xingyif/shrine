package net.shrine.protocol

import org.junit.Test
import net.shrine.util.ShouldMatchersForJUnit
import scala.xml.XML

/**
 * @author clint
 * @date Mar 3, 2014
 */
final class FailureResultTest extends ShouldMatchersForJUnit {
  @Test
  def testXmlRoundTrip {
    val failure = FailureResult(NodeId("X"), new Exception("foo") with scala.util.control.NoStackTrace)
    
    val unmarshalled = FailureResult.fromXml(Set.empty)(XML.loadString(failure.toXmlString)).get
    
    unmarshalled.origin should equal(failure.origin)
    unmarshalled.cause.getMessage.contains("foo") should be(true)
  }
}
