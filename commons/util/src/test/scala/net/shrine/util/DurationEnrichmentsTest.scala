package net.shrine.util

import org.junit.Test

/**
 * @author clint
 * @date Feb 21, 2014
 */
final class DurationEnrichmentsTest extends ShouldMatchersForJUnit {
  import scala.concurrent.duration._
  import DurationEnrichments._
  
  val fiveMinutes = 5.minutes
  
  val fiveMinutesXml = <duration><value>5</value><unit>MINUTES</unit></duration>
  
  @Test 
  def testToXml {
    fiveMinutes.toXml.toString should equal(fiveMinutesXml.toString)
  }
  
  @Test
  def testFromXml {
    Duration.fromXml(fiveMinutesXml).get should equal(fiveMinutes)
  }
  
  @Test
  def testRoundTrip {
    Duration.fromXml(fiveMinutes.toXml).get should equal(fiveMinutes)
  }
}