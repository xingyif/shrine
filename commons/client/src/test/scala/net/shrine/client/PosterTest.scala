package net.shrine.client

import net.shrine.client.EndpointConfig
import net.shrine.util.ShouldMatchersForJUnit
import org.junit.Test
import java.net.URL
import scala.concurrent.duration.Duration

/**
 * @author clint
 * @date Dec 19, 2013
 */
final class PosterTest extends ShouldMatchersForJUnit {
  @Test
  def testPost {
    val url = "http://example.com"

    val data = "foo"

    val httpClient = new MockHttpClient

    val poster = Poster(url, httpClient)

    httpClient.lastInput should be(None)
    httpClient.lastUrl should be(None)

    poster.post("foo")

    httpClient.lastInput should be(Some(data))
    httpClient.lastUrl should be(Some(url))
  }
}