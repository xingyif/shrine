package net.shrine.util

import org.junit.Test
import scala.util.Success
import scala.util.Failure
import scala.util.Try
import scala.util.Success

/**
 * @author clint
 * @since Oct 30, 2012
 */
final class UtilTest extends ShouldMatchersForJUnit{
  
  @Test
  def testToAndFromBase64 {
    import Base64.{toBase64, fromBase64}
    
    val s = "check me out, I'm a string"
      
    val unmarshalledBytes = fromBase64(toBase64(s.getBytes))
    
    new String(unmarshalledBytes) should equal(s)
  }
  
  @Test
  def testParseXmlTime {
    import XmlDateHelper.parseXmlTime
    
    parseXmlTime("").isFailure should be(true)
    parseXmlTime(null).isFailure should be(true)
    parseXmlTime("asdlaflaksdfhlasdfh12345").isFailure should be(true)
    
    val lexicalRep = "2013-11-26T17:22:34.728-05:00"
    
    val Success(parsed) = parseXmlTime(lexicalRep)
    
    parsed.toString should equal(lexicalRep)
    
    val now = XmlDateHelper.now
    
    val Success(parsedNow) = parseXmlTime(now.toString)
    
    parsedNow should equal(now)
  }
  
  @Test
  def testIsValidUrl {
    import UrlCheck.isValidUrl

    isValidUrl(null) should be(false)
    isValidUrl("") should be(false)
    isValidUrl("aksfhkasfh") should be(false)
    isValidUrl("example.com") should be(false)

    isValidUrl("http://example.com") should be(true)
  }
  
  @Test
  def testNow {
    val now = XmlDateHelper.now
    
    now should not be(null)
    
    //TODO: Is there anything more we can do?  
    //Off-by-one and build-server timing issues abound for all the approaches I can think of. :( -Clint 
  }
  
  @Test
  def testTime {
    //verify values are passed through
    val x = 123
    
    val noop: String => Unit = _ => ()
    
    import XmlDateHelper.time
    
    time("Identity")(noop)(x) should be(x)
  }
  
  private val x = 123
  
  private val e = new Exception with scala.util.control.NoStackTrace
  
  @Test
  def testOptionTryImplicits {
    import Tries.Implicits
    
    Implicits.try2Option(Success(x)) should equal(Some(x))
    Implicits.try2Option(Failure(e)) should equal(None)
  }
  
  @Test
  def testSequenceOption {
    import Tries.sequence
    
    sequence(Some(Success(x))) should equal(Success(Some(x)))
    sequence(Some(Failure(e))) should equal(Failure(e))
    
    sequence(None) should be(Success(None))
  }
  
  @Test
  def testSequenceTraversable {
    import Tries.sequence
    
    val y = 234
    val z = 43985
    
    sequence(List(Success(x))) should equal(Success(List(x)))
    sequence(Seq(Failure(e))) should equal(Failure(e))
    
    sequence(Vector(Success(x), Try(y), Try(z))) should equal(Success(Vector(x, y, z)))
    
    sequence(Vector(Failure(e), Try(y), Try(z))) should equal(Failure(e))
    sequence(Vector(Try(x), Failure(e), Try(z))) should equal(Failure(e))
    sequence(Vector(Try(x), Try(y), Failure(e))) should equal(Failure(e))
    
    val f = new Exception with scala.util.control.NoStackTrace
    
    sequence(Seq(Try(x), Failure(e), Failure(f))) should equal(Failure(e))
    sequence(Seq(Failure(f), Failure(e))) should equal(Failure(f))
    
    sequence(Nil) should be(Success(Nil))
  }
  
  @Test
  def testOptionToTry: Unit = {
    import Tries.toTry
    
    toTry(Some(123))(???) should equal(Try(123))
    
    val exception = new Exception
    
    toTry(None)(exception) should equal(Failure(exception))
  }
}