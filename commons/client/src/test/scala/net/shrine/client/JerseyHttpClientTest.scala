package net.shrine.client

import org.junit.Test
import com.sun.jersey.api.client.config.DefaultClientConfig
import com.sun.jersey.client.urlconnection.HTTPSProperties
import net.shrine.util.ShouldMatchersForJUnit
import net.shrine.crypto.KeyStoreCertCollection
import net.shrine.crypto.TrustParam
import TrustParam.AcceptAllCerts
import TrustParam.SomeKeyStore
import net.shrine.crypto.KeyStoreDescriptor
import net.shrine.crypto.TestKeystore
import com.sun.jersey.api.client.config.ClientConfig
import scala.language.reflectiveCalls

/**
 * @author clint
 * @date Aug 2, 2012
 */
final class JerseyHttpClientTest extends ShouldMatchersForJUnit {
  @Test
  def testTrustsAllCertsHostnameVerifier {
    import JerseyHttpClient.TrustsAllCertsHostnameVerifier._

    //These assertions aren't great, but they're about the best we can do;
    //TrustsAllCertsHostnameVerifier should return true for all input
    verify(null, null) should equal(true)
    verify("", null) should equal(true)
    verify("asklfjalksf", null) should equal(true)
  }

  @Test
  def testTrustsAllCertsTrustManager {
    import JerseyHttpClient.TrustsAllCertsTrustManager._

    getAcceptedIssuers should be(null)

    //We can't prove that these two don't have side effects, but we can check that they don't throw 
    checkClientTrusted(Array(), "")
    checkServerTrusted(Array(), "")
  }

  @Test
  def testCreateClientAndWebResource {
    import JerseyHttpClient.createJerseyClient
    import scala.collection.JavaConverters._
    import scala.concurrent.duration._

    def doTest(timeout: Duration) {
      val defaultClientConfig = {
        val config = new DefaultClientConfig

        if (timeout.isFinite) {
          config.getProperties.put(ClientConfig.PROPERTY_CONNECT_TIMEOUT, Long.box(timeout.toMillis))

          config.getProperties.put(ClientConfig.PROPERTY_READ_TIMEOUT, Long.box(timeout.toMillis))
        }

        config
      }

      type HasProperties = {
        def getProperties(): java.util.Map[String, AnyRef]
      }

      import HTTPSProperties.{ PROPERTY_HTTPS_PROPERTIES => httpsPropsKey }

      def doChecksCertsClientTest(client: HasProperties) {
        client should not be (null)

        val clientProps = client.getProperties.asScala
        val propertiesWithoutHttpsProperties = clientProps - httpsPropsKey
        val httpsProperties = clientProps(httpsPropsKey).asInstanceOf[HTTPSProperties]

        //check that we only have default properties plus https_properties
        //turn property maps to Scala maps to get workable equals()
        propertiesWithoutHttpsProperties should equal(defaultClientConfig.getProperties.asScala)

        httpsProperties should not be (null)
        httpsProperties.getHostnameVerifier should be(null)
        httpsProperties.getSSLContext should not be (null)
        httpsProperties.getSSLContext.getProtocol should equal("TLS")
        //TODO: Verify we're using the Spin keystore somehow. 
        //Unfortunately, the contents of the SSLContext are a bit opaque 
      }

      def doTrustsAllCertsClientTest(client: HasProperties) {
        client should not be (null)

        val clientProps = client.getProperties.asScala
        val propertiesWithoutHttpsProperties = clientProps - httpsPropsKey
        val httpsProperties = clientProps(httpsPropsKey).asInstanceOf[HTTPSProperties]

        propertiesWithoutHttpsProperties should equal(defaultClientConfig.getProperties.asScala)

        httpsProperties should not be (null)
        httpsProperties.getHostnameVerifier should be(JerseyHttpClient.TrustsAllCertsHostnameVerifier)
        httpsProperties.getSSLContext should not be (null)
        httpsProperties.getSSLContext.getProtocol should equal("TLS")
        //Would be nice to test that the SSLContext correctly uses TrustsAllCertsTrustManager, but this doesn't seem possible
      }

      val uri = "http://example.com"

      {
        val client = createJerseyClient(TestKeystore.trustParam, timeout)

        doChecksCertsClientTest(client)

        val webResource = client.resource(uri)

        doChecksCertsClientTest(webResource)

        webResource.getURI.toString should equal(uri)
      }

      {
        val client = createJerseyClient(AcceptAllCerts, timeout)

        doTrustsAllCertsClientTest(client)

        val webResource = client.resource(uri)

        doTrustsAllCertsClientTest(webResource)

        webResource.getURI.toString should equal(uri)
      }
    }

    doTest(99.minutes)
    doTest(Duration.Inf)
  }
}