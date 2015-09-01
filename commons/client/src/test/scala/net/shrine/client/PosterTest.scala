package net.shrine.client

import net.shrine.util.ShouldMatchersForJUnit
import org.junit.Test

/**
 * @author clint
 * @since Dec 19, 2013
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