package net.shrine.authorization

import org.junit.Test
import net.shrine.util.ShouldMatchersForJUnit
import net.shrine.client.HttpResponse
import net.shrine.client.Poster

/**
 * @author clint
 * @date Apr 5, 2013
 */
final class PmHttpClientComponentTest extends ShouldMatchersForJUnit {
  @Test
  def testUrlIsSupplied {
    val pmUrl = "lasdjlkasjdlkasjdlkasdjl"
    
    val httpClient = new LazyMockHttpClient("foo")
      
    val component = new PmHttpClientComponent {
      override val pmPoster = Poster(pmUrl, httpClient)
    } 
    
    val payload = "foo"
    
    component.callPm(payload) should equal(HttpResponse(200, "foo"))
    
    httpClient.inputParam should equal(Some(payload))
    
    httpClient.urlParam should equal(Some(pmUrl))
  }
}
