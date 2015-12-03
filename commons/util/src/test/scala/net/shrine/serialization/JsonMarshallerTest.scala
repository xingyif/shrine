package net.shrine.serialization

import org.junit.Test
import net.shrine.util.ShouldMatchersForJUnit

/**
 * @author Bill Simons
 * @date 3/20/12
 * @link http://cbmi.med.harvard.edu
 * @link http://chip.org
 *       <p/>
 *       NOTICE: This software comes with NO guarantees whatsoever and is
 *       licensed as Lgpl Open Source
 * @link http://www.gnu.org/licenses/lgpl.html
 */
final class JsonMarshallerTest extends ShouldMatchersForJUnit {
  @Test
  def testToJsonString: Unit = {
    import net.liftweb.json.JsonDSL._

    val json = new JsonMarshaller {
      def toJson = ("key" -> "value")
    }
    
    json.toJsonString() should equal("{\"key\":\"value\"}")
    json.toJsonString(JsonMarshaller.COMPACT) should equal("{\"key\":\"value\"}")
    json.toJsonString(JsonMarshaller.PRETTY) should equal("{\n  \"key\":\"value\"\n}")
  }

}