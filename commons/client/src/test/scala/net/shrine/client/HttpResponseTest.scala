package net.shrine.client

import net.shrine.util.ShouldMatchersForJUnit
import org.junit.Test

/**
 * @author clint
 * @date Sep 25, 2013
 */
final class HttpResponseTest extends ShouldMatchersForJUnit {
  @Test
  def testOk {
    HttpResponse.ok("foo") should equal(HttpResponse(200, "foo"))
  }
}