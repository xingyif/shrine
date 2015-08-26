package net.shrine.config

import net.shrine.util.ShouldMatchersForJUnit
import org.junit.Test
import com.typesafe.config.ConfigFactory

/**
 * @author clint
 * @date Dec 5, 2013
 */
final class DurationConfigParserTest extends ShouldMatchersForJUnit {
  @Test
  def testApply {
    intercept[Exception] {
      DurationConfigParser(ConfigFactory.empty)
    }
    
    import ConfigFactory.parseString
    
    //two or more units aren't allowed
    intercept[Exception] {
      DurationConfigParser(parseString("""
        milliseconds = 123
        days = 1.23
      """))
    }
    
    intercept[Exception] {
      DurationConfigParser(parseString("""
        milliseconds = 123
        days = 1.23
        seonds = 123.456
      """))
    }
    
    import scala.concurrent.duration._
    
    DurationConfigParser(parseString("milliseconds = 123")) should equal(123.milliseconds)
    DurationConfigParser(parseString("seconds = 99")) should equal(99.seconds)
    DurationConfigParser(parseString("minutes = 123")) should equal(123.minutes)
    DurationConfigParser(parseString("hours = 42")) should equal(42.hours)
    DurationConfigParser(parseString("days = 456")) should equal(456.days)
    
    //Make sure we handle floating-point values too
    DurationConfigParser(parseString("milliseconds = 1.23")) should equal(1.23.milliseconds)
    DurationConfigParser(parseString("seconds = 9.9")) should equal(9.9.seconds)
    DurationConfigParser(parseString("minutes = 12.3")) should equal(12.3.minutes)
    DurationConfigParser(parseString("hours = 4.2")) should equal(4.2.hours)
    DurationConfigParser(parseString("days = 45.6")) should equal(45.6.days)
  }
}