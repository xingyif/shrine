package net.shrine.client

import java.net.URL

import com.typesafe.config.Config
import net.shrine.config.DurationConfigParser

import scala.concurrent.duration.Duration

/**
 * @author clint
 * @since Dec 5, 2013
 */
//todo put all this in EndPoint's apply method
final case class EndpointConfig(url: URL, acceptAllCerts: Boolean, timeout: Duration)

object EndpointConfig {
  object Keys {
    val url = "url"
    val acceptAllCerts = "acceptAllCerts"
    val timeout = "timeout"
  }
  
  val defaultAcceptAllCertsValue = false
  
  import scala.concurrent.duration._
  
  val defaultTimeout = Duration.Inf

  def apply(config: Config): EndpointConfig = {
    val url = new URL(config.getString(Keys.url))
    
    val acceptAllCerts = {
      if(config.hasPath(Keys.acceptAllCerts)) { config.getBoolean(Keys.acceptAllCerts) }
      else { defaultAcceptAllCertsValue }
    }
    
    val timeout = {
      if(config.hasPath(Keys.timeout)) { DurationConfigParser(config.getConfig(Keys.timeout)) }
      else { defaultTimeout }
    }
    
    EndpointConfig(url, acceptAllCerts, timeout)
  }
}