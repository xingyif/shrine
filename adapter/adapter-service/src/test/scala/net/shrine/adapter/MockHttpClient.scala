package net.shrine.adapter

import net.shrine.client.HttpClient
import net.shrine.client.HttpResponse

/**
 * @author clint
 * @date Sep 20, 2012
 */
object MockHttpClient extends HttpClient {
  override def post(input: String, url: String): HttpResponse = HttpResponse.ok("")
    
  def apply(f: => String): HttpClient = new HttpClient {
    override def post(input: String, url: String): HttpResponse = HttpResponse.ok(f)
  }    
}