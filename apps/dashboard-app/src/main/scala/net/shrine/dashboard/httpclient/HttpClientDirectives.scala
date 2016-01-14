package net.shrine.dashboard.httpclient

import java.io.InputStream
import java.security.cert.X509Certificate
import javax.net.ssl.{X509TrustManager, SSLContext}

import net.shrine.log.Loggable
import spray.can.Http
import akka.io.IO
import akka.actor.{ActorRef, ActorSystem}
import spray.can.Http.{HostConnectorSetup, ConnectionAttemptFailedException}
import spray.http.{HttpCredentials, HttpHeaders, HttpHeader, HttpEntity, StatusCodes, HttpRequest, HttpResponse, Uri}
import spray.io.ClientSSLEngineProvider
import spray.routing.{RequestContext, Route}
import akka.pattern.ask

import scala.concurrent.{TimeoutException, Await, Future, blocking}

import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.control.NonFatal

/**
 * From https://github.com/bthuillier/spray/commit/d31fc1b5e1415e1b908fe7d1f01f364a727e2593 with extra bits from http://www.cakesolutions.net/teamblogs/http-proxy-with-spray .
 * Replace when Spray has its own version.
 *
 * @author david
 * @since 9/14/15
 */
trait HttpClientDirectives extends Loggable {

  /**
    * Proxy the request to the specified base uri appended with the unmatched path.
    *
    */
  //todo these implicits don't buy that much . Consider ditching them.
  def forwardUnmatchedPath(baseUri: Uri,maybeCredentials:Option[HttpCredentials] = None)(implicit system: ActorSystem): Route = {
    def completeWithEntityAsString(httpResponse:HttpResponse,uri:Uri):Route = {
      ctx => {
        ctx.complete(httpResponse.entity.asString)
      }
    }
    requestWithUnmatchedPath(baseUri,completeWithEntityAsString,maybeCredentials)
  }

  /**
    * Make the request to the specified base uri appended with the unmatched path, then use the returned entity (as a string) to complete the route.
    *
    */
  def requestWithUnmatchedPath(baseUri:Uri, route:(HttpResponse,Uri) => Route,maybeCredentials:Option[HttpCredentials] = None)(implicit system: ActorSystem): Route = {
    ctx => {
      val resourceUri = baseUri.withPath(baseUri.path.++(ctx.unmatchedPath))
      requestUriThenRoute(resourceUri,route,maybeCredentials)(system)(ctx)
    }
  }

  /**
    * proxy the request to the specified uri with the unmatched path, then use the returned entity (as a string) to complete the route.
    *
    */
  def requestUriThenRoute(resourceUri:Uri, route:(HttpResponse,Uri) => Route,maybeCredentials:Option[HttpCredentials] = None)(implicit system: ActorSystem): Route = {
    ctx => {
      val httpResponse = httpResponseForUri(resourceUri,ctx,maybeCredentials)(system)
      info(s"Got $httpResponse for $resourceUri")

      handleCommonErrorsOrRoute(route)(httpResponse,resourceUri)(ctx)
    }
  }

  private def httpResponseForUri(resourceUri:Uri,ctx: RequestContext,maybeCredentials:Option[HttpCredentials] = None)(implicit system: ActorSystem):HttpResponse = {

    info(s"Requesting $resourceUri")

    if(resourceUri.scheme == "classpath") ClasspathResourceHttpClient.loadFromResource(resourceUri.path.toString())
    else {
      val basicRequest = HttpRequest(ctx.request.method,resourceUri)
      val request = maybeCredentials.fold(basicRequest){ (credentials: HttpCredentials) =>
        val headers: List[HttpHeader] = basicRequest.headers :+ HttpHeaders.Authorization(credentials)
        basicRequest.copy(headers = headers)
      }
      HttpClient.webApiCall(request)
    }
  }

  def handleCommonErrorsOrRoute(route:(HttpResponse,Uri) => Route)(httpResponse: HttpResponse,uri:Uri): Route = {
    ctx => {
      if(httpResponse.status != StatusCodes.OK) {
        //todo create and report a problem
        val ctxCopy: RequestContext = ctx.withHttpResponseMapped(_.copy(status = httpResponse.status))
        ctxCopy.complete(s"$uri replied with $httpResponse")
      }
      else route(httpResponse,uri)(ctx)
    }
  }

}

object HttpClientDirectives extends HttpClientDirectives


/**
  * A simple HttpClient to use inside the HttpDirectives
  */
object HttpClient extends Loggable {

  //todo hand back a Try, Failures with custom exceptions instead of a crappy response
  def webApiCall(request:HttpRequest)(implicit system: ActorSystem): HttpResponse = {
    val transport: ActorRef = IO(Http)(system)

    debug(s"Requesting $request uri is ${request.uri} path is ${request.uri.path}")
    blocking {
      val future:Future[HttpResponse] = for {
        Http.HostConnectorInfo(connector, _) <- transport.ask(createConnector(request))(10 seconds) //todo make this timeout configurable
        response <- connector.ask(request)(10 seconds).mapTo[HttpResponse] //todo make this timeout configurable
      } yield response
      try {
        Await.result(future, 10 seconds)  //todo make this timeout configurable
      }
      catch {
        case x:TimeoutException => HttpResponse(status = StatusCodes.RequestTimeout,entity = HttpEntity(s"${request.uri} timed out after 10 seconds. ${x.getMessage}"))
          //todo is there a better message? What comes up in real life?
        case x:ConnectionAttemptFailedException => {
          //no web service is there to respond
          info(s"${request.uri} failed with ${x.getMessage}",x)
          HttpResponse(status = StatusCodes.NotFound,entity = HttpEntity(s"${request.uri} failed with ${x.getMessage}"))
        }
        case NonFatal(x) => {
          info(s"${request.uri} failed with ${x.getMessage}",x)
          HttpResponse(status = StatusCodes.InternalServerError,entity = HttpEntity(s"${request.uri} failed with ${x.getMessage}"))
        }
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

/**
  * For testing, get an HttpResponse for a classpath resource
  */
object ClasspathResourceHttpClient extends Loggable {

  def loadFromResource(resourceName:String):HttpResponse = {
    blocking {
      val cleanResourceName = if (resourceName.startsWith ("/") ) resourceName.drop(1)
                              else resourceName
      val classLoader = getClass.getClassLoader
      try {
        val is: InputStream = classLoader.getResourceAsStream (cleanResourceName)
        val string:String = scala.io.Source.fromInputStream (is).mkString
        HttpResponse(entity = HttpEntity(string))
      }
      catch{
        case NonFatal(x) => {
          info(s"Could not load $resourceName",x)
          HttpResponse(status = StatusCodes.NotFound,entity = HttpEntity(s"Could not load $resourceName due to ${x.getMessage}"))
        }
      }
    }
  }
}