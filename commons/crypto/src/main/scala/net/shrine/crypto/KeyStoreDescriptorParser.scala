package net.shrine.crypto

import com.typesafe.config.{Config, ConfigValue, ConfigValueType}

import net.shrine.config.ConfigExtensions
import net.shrine.log.Loggable
import scala.collection.JavaConverters._

/**
 * @author clint
 * @since Dec 9, 2013
 */
object KeyStoreDescriptorParser extends Loggable {
  object Keys {
    val file = "file"
    val password = "password"
    val privateKeyAlias = "privateKeyAlias"
    val keyStoreType = "keyStoreType"
    val caCertAliases = "caCertAliases"
  }

  def apply(config: Config): KeyStoreDescriptor = {

    import Keys._
    
    def getKeyStoreType: KeyStoreType = {
      val typeOption = config.getOption(keyStoreType,_.getString)

      typeOption.flatMap(KeyStoreType.valueOf).getOrElse {
        info(s"Unknown keystore type '${typeOption.getOrElse("")}', allowed types are ${KeyStoreType.JKS.name} and ${KeyStoreType.PKCS12.name}")
        
        KeyStoreType.Default
      }
    }
    
    def getCaCertAliases: Seq[String] = {

      def isString(cv: ConfigValue) = cv.valueType == ConfigValueType.STRING

      config.getOption(caCertAliases,_.getList).fold(Seq.empty[ConfigValue])(list => list.asScala).collect{ case cv if isString(cv) => cv.unwrapped.toString }
    }
    
    KeyStoreDescriptor(
        config.getString(file), 
        config.getString(password),
        config.getOption(privateKeyAlias,_.getString),
        getCaCertAliases,
        getKeyStoreType)
  }
}