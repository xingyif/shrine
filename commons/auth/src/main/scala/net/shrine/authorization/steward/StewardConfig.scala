package net.shrine.authorization.steward

import java.net.URL

import com.typesafe.config.Config

import net.shrine.config.ConfigExtensions

/**
 * @author david 
 * @since 7/21/15
 */
final case class StewardConfig(qepUserName:String,qepPassword:String,stewardBaseUrl:URL)

object StewardConfig {

  object Keys {
    val qepUserName = "qepUserName"
    val qepPassword = "qepPassword"
    val stewardBaseUrl = "stewardBaseUrl"
  }

  def apply(config: Config): StewardConfig = {

    StewardConfig(
      config.getString(Keys.qepUserName),
      config.getString(Keys.qepPassword),
      config.get(Keys.stewardBaseUrl, new URL(_))
    )
  }
}