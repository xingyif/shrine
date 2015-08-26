package net.shrine.protocol

import com.typesafe.config.Config

import net.shrine.config.ConfigExtensions

/**
 * @author clint
 * @since Dec 5, 2013
 */
final case class CredentialConfig(domain: Option[String], username: String, password: String)

object CredentialConfig {
  object Keys {
    val domain = "domain"
    val username = "username"
    val password = "password"
  }
  
  def apply(config: Config): CredentialConfig = {
    val domain = config.getOption(Keys.domain,_.getString)
    val username = config.getString(Keys.username)
    val password = config.getString(Keys.password)
    
    CredentialConfig(domain, username, password)
  }
}