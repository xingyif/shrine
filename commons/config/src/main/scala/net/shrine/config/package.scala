package net.shrine

import com.typesafe.config.Config

package object config {

  /**
   * @author dwalend
   * @since July 17, 2015
   *
   * Helper methods for parsing com.typesafe.config.Config objects
   */
  implicit class ConfigExtensions(self: Config) {

    def get[T](key:String,construct:String => T):T = construct(self.getString(key))

    def getConfigured[T](key: String, constructor: Config => T): T = constructor(self.getConfig(key))

    def getOption[T](key: String, extract: Config => (String => T)): Option[T] = {
      if (self.hasPath(key)) Option(extract(self)(key))
      else None
    }

    def getOptionConfigured[T](key: String, constructor: Config => T): Option[T] = {
      getOption(key, _.getConfig).map(constructor)
    }

  }

}