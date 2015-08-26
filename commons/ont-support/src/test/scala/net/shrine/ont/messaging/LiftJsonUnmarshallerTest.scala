package net.shrine.ont.messaging

import org.junit.Test
import net.liftweb.json._
import net.liftweb.json.JsonDSL._

/**
 * @author Clint Gilbert
 * @date Feb 8, 2012
 */
@Test
final class LiftJsonUnmarshallerTest extends FromJsonTest(LiftJsonUnmarshallerTest) {
  @Test
  def testFromJson = {
    def blarg = Blarg(123, -123.456D)
    
    def uuid = java.util.UUID.randomUUID.toString
    
    val foo = Foo(99, uuid, (1 to 5).map(_ => blarg))
    
    doTestFromJson(foo)
  }
}

object LiftJsonUnmarshallerTest extends LiftJsonUnmarshaller[Foo]
