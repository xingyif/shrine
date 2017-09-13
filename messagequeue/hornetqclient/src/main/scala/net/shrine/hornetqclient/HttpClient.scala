package net.shrine.hornetqclient

import java.security.cert.X509Certificate
import javax.net.ssl.{SSLContext, X509TrustManager}
import akka.actor.{ActorRef, ActorSystem}
import akka.io.IO
import akka.pattern.ask
import net.shrine.log.Loggable
import net.shrine.source.ConfigSource
import net.shrine.config.ConfigExtensions
import spray.can.Http
import spray.can.Http.{ConnectionAttemptFailedException, HostConnectorSetup}
import spray.http.{HttpEntity, HttpRequest, HttpResponse, StatusCodes}
import spray.io.ClientSSLEngineProvider

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{Duration, DurationInt, DurationLong}
import scala.concurrent.{Await, Future, TimeoutException}
import scala.language.postfixOps
import scala.util.control.NonFatal

/**
  * A simple HttpClient to use inside the HttpDirectives
  */
object HttpClient extends Loggable {

  //todo hand back a Try, Failures with custom exceptions instead of a crappy response
  //todo Really a Future would be even better
  def webApiCall(request:HttpRequest,
                 timeout:Duration = ConfigSource.config.get("shrine.messagequeue.httpClient.defaultTimeOutSecond", Duration(_)))
                (implicit system: ActorSystem): HttpResponse = {

    val deadline = System.currentTimeMillis() + timeout.toMillis

    val transport: ActorRef = IO(Http)(system)

    debug(s"Requesting $request uri is ${request.uri} path is ${request.uri.path}")
    val future: Future[HttpResponse] = for {
      Http.HostConnectorInfo(connector, _) <- transport.ask(createConnector(request))(deadline - System.currentTimeMillis() milliseconds)
      response <- connector.ask(request)(deadline - System.currentTimeMillis() milliseconds).mapTo[HttpResponse]
    } yield response
    try {
      //wait a second longer than the deadline before timing out the Await, to let the actors timeout
      Await.result(future, deadline + 1000 - System.currentTimeMillis() milliseconds) //todo make this time gap configurable SHRINE-2217
    }
    catch {
      //todo definitely need the Try instead of this sloppy replacement of the HttpResponse.
      case x: TimeoutException => {
        debug(s"${request.uri} failed with ${x.getMessage}", x)
        HttpResponse(status = StatusCodes.RequestTimeout, entity = HttpEntity(s"${request.uri} timed out after ${timeout}. ${x.getMessage}"))
      }
      //todo is there a better message? What comes up in real life?
      case x: ConnectionAttemptFailedException => {
        //no web service is there to respond
        debug(s"${request.uri} failed with ${x.getMessage}", x)
        HttpResponse(status = StatusCodes.NotFound, entity = HttpEntity(s"${request.uri} failed with ${x.getMessage}"))
      }
      case NonFatal(x) => {
        debug(s"${request.uri} failed with ${x.getMessage}", x)
        HttpResponse(status = StatusCodes.InternalServerError, entity = HttpEntity(s"${request.uri} failed with ${x.getMessage}"))
      }
    }
  }

  //from https://github.com/TimothyKlim/spray-ssl-poc/blob/master/src/main/scala/Main.scala
  //trust all SSL contexts. We just want encrypted comms.
  implicit val trustfulSslContext: SSLContext = {

    class IgnoreX509TrustManager extends X509TrustManager {
      def checkClientTrusted(chain: Array[X509Certificate], authType: String) {}

      def checkServerTrusted(chain: Array[X509Certificate], authType: String) {}

      def getAcceptedIssuers = null
    }

    val context = SSLContext.getInstance("TLS")
    context.init(null, Array(new IgnoreX509TrustManager), null)
    info("trustfulSslContex initialized")
    context
  }

  implicit val clientSSLEngineProvider =
  //todo lookup this constructor
    ClientSSLEngineProvider {
      _ =>
        val engine = trustfulSslContext.createSSLEngine()
        engine.setUseClientMode(true)
        engine
    }

  def createConnector(request: HttpRequest) = {

    val connector = new HostConnectorSetup(host = request.uri.authority.host.toString,
      port = request.uri.effectivePort,
      sslEncryption = request.uri.scheme == "https",
      defaultHeaders = request.headers)
    connector
  }

}
