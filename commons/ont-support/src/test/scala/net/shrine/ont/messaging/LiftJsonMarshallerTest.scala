package net.shrine.ont.messaging

import org.junit.Test
import net.shrine.util.ShouldMatchersForJUnit
import net.liftweb.json.JsonDSL._
import net.liftweb.json.JValue

/**
 * @author Clint Gilbert
 * @date Feb 8, 2012
 */
@Test
final class LiftJsonMarshallerTest extends ShouldMatchersForJUnit {
  private val foo = Foo(123, "askdj", Seq(Blarg(123, 4.56D), Blarg(99, -1.23D)))
  
  @Test
  def testToJson {
    val expectedJson = "{\"bar\":123,\"baz\":\"askdj\",\"blargs\":[{\"x\":123,\"y\":4.56},{\"x\":99,\"y\":-1.23}]}"
      
    foo.toJsonString() should equal(expectedJson)
  }
  
  @Test
  def testToJValue {
    val expectedJValue: JValue = ("bar" -> foo.bar) ~ ("baz" -> foo.baz) ~ ("blargs" -> foo.blargs.map(_.toJson))
    
    foo.toJson should equal(expectedJValue)
  }
}