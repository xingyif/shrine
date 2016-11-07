package net.shrine.crypto

import com.typesafe.config.{Config, ConfigValue, ConfigValueType}
import net.shrine.config.ConfigExtensions
import net.shrine.log.Loggable
import net.shrine.util.{PeerToPeerModel, SingleHubModel, TrustModel}

import scala.collection.JavaConverters._

/**
 * @author clint
 * @since Dec 9, 2013
 */
object KeyStoreDescriptorParser extends Loggable {
  object Keys {
    val file            = "file"
    val password        = "password"
    val privateKeyAlias = "privateKeyAlias"
    val keyStoreType    = "keyStoreType"
    val caCertAliases   = "caCertAliases"
    val trustModel      = "trustModelIsHub"
    val isHub           = "isHub"
    val qepEndpoint     = "broadcasterServiceEndpoint"
    val url             = "url"
    val downStreamNodes = "downstreamNodes"
    val aliasMap        = "aliasMap"
  }

  def apply(keyStoreConfig: Config, hubConfig: Config, qepConfig: Config): KeyStoreDescriptor = {
    import Keys._
    import scala.collection.JavaConversions._

    def getTrustModel: TrustModel = {
      val hasModel = keyStoreConfig.hasPath(trustModel)
      if (hasModel && !keyStoreConfig.getBoolean(trustModel))
        PeerToPeerModel
      else if (hasModel && keyStoreConfig.hasPath(isHub))
        SingleHubModel(keyStoreConfig.getBoolean(isHub))
      else if (hasModel) {
        warn(s"Did not specify whether this is the hub or a downStreamNode, assuming it ${if (hubConfig.isEmpty) "isn't" else "is"} because the hub config is ${if (hubConfig.isEmpty) "empty" else "defined"}")
        SingleHubModel(!hubConfig.isEmpty)
      } else {
        info("No Trust Model specified for this network configuration, assuming that a PeerToPeer configuration is being used")
        PeerToPeerModel
      }
    }

    val aliases = keyStoreConfig.getConfig(aliasMap).entrySet()
    val tm = getTrustModel

    def parseUrl(url: String): String = {
      url.split("(https://)|(:.*)")(1)
    }

    def getRemoteSites: Seq[RemoteSiteDescriptor] = {
      tm match {
        case SingleHubModel(true) | PeerToPeerModel => parseAliasMap
        case SingleHubModel(false) => parseSimpleMap
      }
    }

    def parseSimpleMap: Seq[RemoteSiteDescriptor] = {
      assert(aliases.size() == 1, "In a DownStreamNode, the only remote site is the hub") // Todo: Better error handling
      val entry = aliases.head
      assert(entry.getValue.valueType() == ConfigValueType.STRING)

      Seq(RemoteSiteDescriptor(entry.getKey, cvToString(entry.getValue), parseUrl(qepConfig.getString(s"$qepEndpoint.$url"))))
    }

    def parseAliasMap: Seq[RemoteSiteDescriptor] = {
      val downStreamAliases = hubConfig.getConfig(downStreamNodes).entrySet()
      assert(aliases.size() == downStreamAliases.size(), "The aliasMap has to match one-to-one with the Hub's downstreamNodes")
      (aliases ++ downStreamAliases).foreach(entry => assert(entry.getValue.valueType() == ConfigValueType.STRING))
      assert(aliases.size == aliases.map(_.getKey).intersect(downStreamAliases.map(_.getKey)).size)

      aliases.map(siteAlias =>
        RemoteSiteDescriptor(
          siteAlias.getKey,
          cvToString(siteAlias.getValue),
          parseUrl(cvToString(downStreamAliases.find(_.getKey == siteAlias.getKey).get.getValue)))).toSeq
    }

    def getKeyStoreType: KeyStoreType = {
      val typeOption = keyStoreConfig.getOption(keyStoreType,_.getString)

      typeOption.flatMap(KeyStoreType.valueOf).getOrElse {
        info(s"Unknown keystore type '${typeOption.getOrElse("")}', allowed types are ${KeyStoreType.JKS.name} and ${KeyStoreType.PKCS12.name}")

        KeyStoreType.Default
      }
    }

    def cvToString(cv: ConfigValue): String = {
      cv.unwrapped.toString
    }
    def getCaCertAliases: Seq[String] = {

      def isString(cv: ConfigValue) = cv.valueType == ConfigValueType.STRING

      keyStoreConfig.getOption(caCertAliases,_.getList).fold(Seq.empty[ConfigValue])(list => list.asScala).collect{ case cv if isString(cv) => cvToString(cv) }
    }

    KeyStoreDescriptor(
      keyStoreConfig.getString(file),
      keyStoreConfig.getString(password),
      keyStoreConfig.getOption(privateKeyAlias, _.getString),
      getCaCertAliases,
      getKeyStoreType,
      tm,
      getRemoteSites
    )
  }
}

case class RemoteSiteDescriptor(siteAlias: String, keyStoreAlias: String, url: String)