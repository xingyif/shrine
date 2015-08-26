package net.shrine.ont.messaging

import org.junit.Test
import net.shrine.ont.OntTerm

/**
 * @author Dave Ortiz
 * @date 11/3/11
 * @link http://cbmi.med.harvard.edu
 * @link http://chip.org
 *       <p/>
 *       NOTICE: This software comes with NO guarantees whatsoever and is
 *       licensed as Lgpl Open Source
 * @link http://www.gnu.org/licenses/lgpl.html
 */
@Test
final class SearchResponseTest extends FromJsonTest(SearchResponse) {

  import OntTerm.shrinePrefix
  
  val concept1 = Concept(shrinePrefix + """category1\concept1Key""", None)
  val concept2 = Concept(shrinePrefix + """category1\concept2Key""", None)
  val concept3 = Concept(shrinePrefix + """category1\concept3Key""", None)

  val searchResponse = SearchResponse("test", Seq(concept1, concept2, concept3))
  
  @Test
  def testToJsonString {
    searchResponse.toJsonString() should equal("""{"originalQuery":"test","concepts":[{"path":"\\\\SHRINE\\SHRINE\\category1\\concept1Key","category":"category1","simpleName":"concept1Key"},{"path":"\\\\SHRINE\\SHRINE\\category1\\concept2Key","category":"category1","simpleName":"concept2Key"},{"path":"\\\\SHRINE\\SHRINE\\category1\\concept3Key","category":"category1","simpleName":"concept3Key"}]}""")
  }
  
  @Test
  def testFromJson = doTestFromJson(searchResponse)
}