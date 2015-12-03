package net.shrine.authorization

import net.shrine.client.HttpClient
import net.shrine.client.HttpResponse

/**
 * @author clint
 * @date Apr 5, 2013
 */
final class LazyMockHttpClient(toReturn: => String) extends HttpClient {
  var urlParam: Option[String] = None
  var inputParam: Option[String] = None

  override def post(input: String, url: String): HttpResponse = {
    this.inputParam = Some(input)
    this.urlParam = Some(url)

    HttpResponse.ok(toReturn)
  }
}