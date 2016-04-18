package net.shrine.client

/**
 * @author clint
 * @since Dec 18, 2013
 */

//todo add an apply(Config based on EndpointConfig)
final case class Poster(url: String, httpClient: HttpClient) {
  def post(data: String): HttpResponse = httpClient.post(data, url)
  
  def mapUrl(f: String => String): Poster = copy(url = f(url))
}
/*
object Poster {
  def apply():Poster = {

  }
}
*/