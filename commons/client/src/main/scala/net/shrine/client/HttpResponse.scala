package net.shrine.client

/**
 * @author clint
 * @date Sep 24, 2013
 */
final case class HttpResponse(statusCode: Int, body: String) {
  def mapBody(f: String => String): HttpResponse = {
    copy(body = f(body))
  }
}

object HttpResponse {
  def ok(body: String): HttpResponse = new HttpResponse(200, body)
}