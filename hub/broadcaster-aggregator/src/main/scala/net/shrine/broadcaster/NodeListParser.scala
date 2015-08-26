package net.shrine.broadcaster

import java.net.URL

import com.typesafe.config.Config
import net.shrine.protocol.NodeId

/**
 * @author clint
 * @since Nov 21, 2013
 * 
 * Maps descriptive human-readable node names => node URIs 
 */
object NodeListParser {
  def apply(config: Config): Iterable[IdAndUrl] = {
    import net.shrine.util.UrlCheck.isValidUrl

    import scala.collection.JavaConverters._
    
    def trimQuotes(s: String) = {
      val handledLeading = if(s.startsWith("\"")) s.drop(1) else s
      
      if(handledLeading.endsWith("\"")) handledLeading.dropRight(1) else handledLeading 
    }
    
    val names = config.entrySet.asScala.map(_.getKey).map(trimQuotes)
    
    val nodeIdsToUrlStrings = names.map(name => NodeId(name) -> config.getString(name)).toMap
    
    val invalidUrls = nodeIdsToUrlStrings.values.filterNot(isValidUrl)
    
    if(invalidUrls.nonEmpty) {
      //Show all unparseable URLs, not just the first one
      throw new IllegalArgumentException(s"Invalid urls: $invalidUrls")
    }
    
    nodeIdsToUrlStrings.map { case (nodeId, url) => IdAndUrl(nodeId, new URL(url))}
  }
}