package net.shrine.client

import java.security.{KeyStore, SecureRandom}
import java.security.cert.X509Certificate
import javax.net.ssl._
import javax.ws.rs.core.MediaType

import com.sun.jersey.api.client.{Client, ClientResponse, WebResource}
import com.sun.jersey.api.client.config.{ClientConfig, DefaultClientConfig}
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter
import com.sun.jersey.client.urlconnection.HTTPSProperties
import net.shrine.crypto.TrustParam.{AcceptAllCerts, BouncyKeyStore}
import net.shrine.crypto.{BouncyKeyStoreCollection, TrustParam}
import net.shrine.log.{Log, Loggable}
import net.shrine.util.{StringEnrichments, XmlUtil}

import scala.concurrent.duration._
import scala.util.Try

/**
 * @author Bill Simons
 * @author clint
 *
 * @since Sep 20, 2012
 * @see http://cbmi.med.harvard.edu
 * @see http://chip.org
 * <p/>
 * NOTICE: This software comes with NO guarantees whatsoever and is
 * licensed as Lgpl Open Source
 * @link http://www.gnu.org/licenses/lgpl.html
 *
 * NB: text/xml is the default mediatype to support i2b2, which apparently requires this media type, and the majority
 * of HttpClients created by Shrine are used to talk to i2b2 services.
 *
 * TODO: Allow specifying credentials, to allow unit testing the Sheriff client classes.
 */
final case class JerseyHttpClient(trustParam: TrustParam,
                                  timeout: Duration,
                                  mediaType: String = MediaType.TEXT_XML,
                                  credentials: Option[HttpCredentials] = None)
  extends HttpClient with Loggable
{
  import JerseyHttpClient._

  private lazy val client = createJerseyClient(trustParam, timeout)

  override def post(input: String, url: String): HttpResponse = {
    def prettyPrintIfXml(s: String): String = {
      import StringEnrichments._

      s.tryToXml.map(_.head).map(XmlUtil.prettyPrint).getOrElse(s)
    }

    debug(s"Invoking '$url' with '${prettyPrintIfXml(input)}'") //todo log the input when safe

    val resp = createJerseyResource(client, url, credentials).entity(input, mediaType).post(classOf[ClientResponse])

    val httpResponse = HttpResponse(resp.getStatus, resp.getEntity(classOf[String]))

    //not safe to call prettyPrintIfXml for an html page. No telling what you've got on most error codes 404.
    //todo someday log when safe
    if(httpResponse.statusCode < 400) debug(s"Got response from '$url' of ${httpResponse.mapBody(b => s"'${ prettyPrintIfXml(b) }'")}")
    else debug(s"Got error code ${httpResponse.statusCode} from '$url' of '${httpResponse.body}'")

    httpResponse
  }
}

object JerseyHttpClient {

  //todo take a config instead of an EndpointConfig
  def apply(keystoreCertCollection: BouncyKeyStoreCollection, endpoint: EndpointConfig):JerseyHttpClient = {
    val trustParam = if (endpoint.acceptAllCerts) AcceptAllCerts else BouncyKeyStore(keystoreCertCollection)
    JerseyHttpClient(trustParam, endpoint.timeout)
  }

  private[client] object TrustsAllCertsHostnameVerifier extends HostnameVerifier {
    override def verify(s: String, sslSession: SSLSession) = true
  }

  private[client] object TrustsAllCertsTrustManager extends X509TrustManager {
    override def getAcceptedIssuers(): Array[X509Certificate] = null
    override def checkClientTrusted(certs: Array[X509Certificate], authType: String): Unit = ()
    override def checkServerTrusted(certs: Array[X509Certificate], authType: String): Unit = ()
  }

  /**
   * From a SO post inspired from http://java.sun.com/javase/6/docs/technotes/guides/security/jsse/JSSERefGuide.html
   */
  private[client] def trustManager(keystore: KeyStore): X509TrustManager = {
    //The Spin PKIX X509TrustManager that we will delegate to.
    val trustManagerFactory: TrustManagerFactory = TrustManagerFactory.getInstance("PKIX")

    trustManagerFactory.init(null:java.security.KeyStore)
    val caCerts =
      trustManagerFactory.getTrustManagers.collect {
        case trustManager: X509TrustManager => trustManager
      }.headOption.getOrElse {
        throw new IllegalStateException("Couldn't initialize SSL TrustManager: No X509TrustManagers found")
      }

    trustManagerFactory.init(keystore)
    val customCerts =
      trustManagerFactory.getTrustManagers.collect {
        case trustManager: X509TrustManager => trustManager
      }.headOption.getOrElse {
        throw new IllegalStateException("Couldn't initialize SSL TrustManager: No X509TrustManagers found")
      }

    new X509TrustManager {
      override def checkServerTrusted(x509Certificates: Array[X509Certificate], s: String): Unit = {
        val custom = Try(customCerts.checkServerTrusted(x509Certificates, s))
        if (custom.isFailure) {
          val ca = Try(caCerts.checkServerTrusted(x509Certificates, s))
          if (ca.isFailure) {
            Log.error(this, custom.failed.get)
            Log.error(this, ca.failed.get)
            throw ca.failed.get
          }
        }
      }

      override def checkClientTrusted(x509Certificates: Array[X509Certificate], s: String): Unit = {
        val custom = Try(customCerts.checkClientTrusted(x509Certificates, s))
        if (custom.isFailure) {
          val ca = Try(caCerts.checkClientTrusted(x509Certificates, s))
          if (ca.isFailure) {
            Log.error(this, custom.failed.get)
            Log.error(this, ca.failed.get)
            throw ca.failed.get
          }
        }
      }

      override def getAcceptedIssuers: Array[X509Certificate] =
        customCerts.getAcceptedIssuers ++ caCerts.getAcceptedIssuers
    }
  }

  /**
   * From a SO post inspired from http://java.sun.com/javase/6/docs/technotes/guides/security/jsse/JSSERefGuide.html
   */
  private[client] def keyManager(keystore: KeyStore, password: Array[Char]): X509KeyManager = {

    //The Spin PKIX X509KeyManager that we will delegate to.
    val keyManagerFactory: KeyManagerFactory = KeyManagerFactory.getInstance("SunX509", "SunJSSE")

    keyManagerFactory.init(keystore, password)

    keyManagerFactory.getKeyManagers.collect {
      case keyManager: X509KeyManager => keyManager
    }.headOption.getOrElse {
      throw new IllegalStateException("Couldn't initialize SSL KeyManager: No X509KeyManagers found")
    }
  }

  def createJerseyResource(client: Client, url: String, credentials: Option[HttpCredentials]): WebResource = {
    val resource = client.resource(url)

    for {
      HttpCredentials(username, password) <- credentials
    } {
      resource.addFilter(new HTTPBasicAuthFilter(username, password))
    }

    resource
  }

  def createJerseyClient(trustParam: TrustParam, timeout: Duration/* = 5.minutes*/): Client = {
    def tlsContext = SSLContext.getInstance("TLS")

    val (sslContext, hostNameVerifier) = {
      val context = tlsContext

      trustParam match {
        case AcceptAllCerts => {
          context.init(null, Array[TrustManager](TrustsAllCertsTrustManager), new SecureRandom)

          (context, TrustsAllCertsHostnameVerifier)
        }
        case BouncyKeyStore(certs) => { // Noo! My beautiful layer of abstraction over the KeyStore!
                                        // Todo: Mock the x509 keyStore manager?
          context.init(Array(keyManager(certs.keyStore, certs.descriptor.password.toCharArray)), Array(trustManager(certs.keyStore)), null)

          (context, null.asInstanceOf[HostnameVerifier])
        }
      }
    }

    val httpsProperties = new HTTPSProperties(hostNameVerifier, sslContext)

    val config: ClientConfig = new DefaultClientConfig

    config.getProperties.put(HTTPSProperties.PROPERTY_HTTPS_PROPERTIES, httpsProperties)

    //Specify the timeout only if it's finite.  Not only will toMillis fail on an infinite duration, but
    //Jersey's default is an infinite timeout; by not specifiying a timeout, we use the default.
    if (timeout.isFinite) {
      val timeoutAsBoxedInt: java.lang.Integer = Int.box(timeout.toMillis.toInt)

      //NB: Jersey requires that these be boxed java.lang.Integers :\
      config.getProperties.put(ClientConfig.PROPERTY_CONNECT_TIMEOUT, timeoutAsBoxedInt)

      //NB: Jersey requires that these be boxed java.lang.Integers :\
      config.getProperties.put(ClientConfig.PROPERTY_READ_TIMEOUT, timeoutAsBoxedInt)
    }

    Client.create(config)
  }
}
