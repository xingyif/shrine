package net.shrine.utilities.jerseyhttpclienttool

import com.typesafe.config.ConfigFactory
import net.shrine.client.{EndpointConfig, JerseyHttpClient}
import net.shrine.crypto.{BouncyKeyStoreCollection, KeyStoreDescriptorParser}
import net.shrine.config.ConfigExtensions

import scala.util.control.NonFatal


/**
 * @author dwalend
 * @since 1.22.5
 */

object JerseyHttpClientTool {
  def main(args: Array[String]): Unit = {

    val config = ConfigFactory.load()

    println(config)

    val shrineConfig = config.getConfig("shrine")
    val qepConfig = shrineConfig.getConfig("queryEntryPoint")

    val endpoint = EndpointConfig(qepConfig.getConfig("broadcasterServiceEndpoint"))

    lazy val keyStoreDescriptor = KeyStoreDescriptorParser(shrineConfig.getConfig("keystore"),
                                                            shrineConfig.getConfigOrEmpty("hub"),
                                                            shrineConfig.getConfigOrEmpty("queryEntryPoint"))

    val certCollection: BouncyKeyStoreCollection = BouncyKeyStoreCollection.fromFileRecoverWithClassPath(keyStoreDescriptor)

    val httpClient = JerseyHttpClient(certCollection, endpoint)

    try {
      httpClient.post(s"test", endpoint.url.toString)
    }
    catch {
      case NonFatal(x) => x.printStackTrace()
    }
  }
}