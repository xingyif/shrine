package net.shrine.util

/**
 * @author clint
 * @since Oct 18, 2012
 */
object UrlCheck {

  def isValidUrl(url: String): Boolean = {
    import java.net

    try {
      new net.URL(url)

      true
    } catch {
      case e: net.MalformedURLException => false
    }
  }
}