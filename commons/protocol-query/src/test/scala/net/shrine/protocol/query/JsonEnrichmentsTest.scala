package net.shrine.protocol.query

import net.shrine.util.ShouldMatchersForJUnit
import org.junit.Test
//import net.liftweb.json.JsonAST._
import net.liftweb.json.JsonDSL._
import net.liftweb.json._
import scala.util.Try

/**
 * @author clint
 * @date Dec 3, 2014
 */
final class JsonEnrichmentsTest extends ShouldMatchersForJUnit {
  import JsonEnrichments._
  
  private val json = parse("""{"x":"123","y":"abc"}""")
  
  val json2 = JObject(List(JField("x", JString("123")), JField("z", JNothing)))
  
  @Test
  def testWithChildString: Unit = {
    json.withChildString("z").isFailure should be(true)
    json.withChildString("x").get should equal("123")
    json.withChildString("y").get should equal("abc")
  }
  
  @Test
  def testWithChildTryExtractor: Unit = {
    def tryToInt(jstring: JString): Try[Int] = Try(jstring.values.toInt)
    
    json.withChild("z", tryToInt(_)).isFailure should be(true)
    json.withChild("x", tryToInt(_)).get should equal(123)
    json.withChild("y", tryToInt(_)).isFailure should be(true)
    
    json2.withChild("x", tryToInt(_)).get should be(123)
    json2.withChild("z", tryToInt(_)).isFailure should be(true)
  }
  
  @Test
  def testWithChildOptionExtractor: Unit = {
    def toInt(jstring: JString): Option[Int] = Try(jstring.values.toInt).toOption
    
    json.withChild("z", toInt(_)).isFailure should be(true)
    json.withChild("x", toInt(_)).get should equal(123)
    json.withChild("y", toInt(_)).isFailure should be(true)
    
    json2.withChild("x", toInt(_)).get should be(123)
    json2.withChild("z", toInt(_)).isFailure should be(true)
  }
}