package net.shrine.messagequeueclient

import java.security.cert.X509Certificate
import javax.net.ssl.{SSLContext, X509TrustManager}

import akka.actor.{ActorRef, ActorSystem}
import akka.io.IO
import akka.pattern.ask
import net.shrine.config.ConfigExtensions
import net.shrine.log.Loggable
import net.shrine.source.ConfigSource
import spray.can.Http
import spray.can.Http.{ConnectionAttemptFailedException, HostConnectorSetup}
import spray.http.{HttpRequest, HttpResponse}
import spray.io.ClientSSLEngineProvider

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{Duration, DurationLong}
import scala.concurrent.{Await, Future, TimeoutException}
import scala.language.postfixOps
import scala.util.Try
import scala.util.control.NonFatal

/**
  * A simple HttpClient to use inside the HttpDirectives
  */
object HttpClient extends Loggable {

  def webApiFuture(request:HttpRequest,
                   timeout:Duration = ConfigSource.config.get("shrine.messagequeue.httpClient.defaultTimeOut", Duration(_))) //todo do I want this default around??
                  (implicit system: ActorSystem):Future[HttpResponse] = {

    val deadline = System.currentTimeMillis() + timeout.toMillis

    val transport: ActorRef = IO(Http)(system)

    debug(s"Requesting $request uri is ${request.uri} path is ${request.uri.path}")
    val future: Future[HttpResponse] = for {
      Http.HostConnectorInfo(connector, _) <- transport.ask(createConnector(request))(deadline - System.currentTimeMillis() milliseconds)
      response <- connector.ask(request)(deadline - System.currentTimeMillis() milliseconds).mapTo[HttpResponse]
    } yield response

    future.transform({s => s}, {
      case NonFatal(x) =>
        debug(s"${request.uri} failed with ${x.getMessage}",x)
        x match {
            //Keeping these bits here to track common things that might go wrong. Too low-level to generate a full problem
        case tx: TimeoutException => tx //something timed out
        case cafx: ConnectionAttemptFailedException => cafx //no web service was there to respond
        case nonFatal => nonFatal //something else went wrong
      }
      case fatal => fatal //don't touch fatal exceptions
    })
  }

  def webApiTry(request:HttpRequest,
                timeout:Duration = ConfigSource.config.get("shrine.messagequeue.httpClient.defaultTimeOut", Duration(_)))
               (implicit system: ActorSystem): Try[HttpResponse] = Try {
    val deadline = System.currentTimeMillis() + timeout.toMillis
    //wait just a little longer than the deadline to let the other timeouts happen first
    val timeOutWaitGap = ConfigSource.config.get("shrine.messagequeue.httpClient.timeOutWaitGap", Duration(_)).toMillis
    Await.result(webApiFuture(request,timeout)(system),deadline + timeOutWaitGap - System.currentTimeMillis() milliseconds)
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

  def createConnector(request: HttpRequest): HostConnectorSetup = {

    val connector = HostConnectorSetup(host = request.uri.authority.host.toString,
      port = request.uri.effectivePort,
      sslEncryption = request.uri.scheme == "https",
      defaultHeaders = request.headers)
    connector
  }

}
