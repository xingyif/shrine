package net.shrine.client

import net.shrine.crypto2.BouncyKeyStoreCollection

/**
 * @author clint
 * @since Dec 18, 2013
 */

//todo add an apply(Config based on EndpointConfig)
final case class Poster(url: String, httpClient: HttpClient) {
  def post(data: String): HttpResponse = httpClient.post(data, url)
  
  def mapUrl(f: String => String): Poster = copy(url = f(url))
}

object Poster {
  //todo a version based on config
  def apply(keystoreCertCollection: BouncyKeyStoreCollection, endpoint: EndpointConfig):Poster = {

    val httpClient = JerseyHttpClient(keystoreCertCollection, endpoint)

    Poster(endpoint.url.toString, httpClient)
  }
}
