package net.shrine.dashboard.httpclient

import java.io.InputStream

import akka.actor.ActorSystem
import net.shrine.hornetqclient.HttpClient
import net.shrine.log.Loggable
import net.shrine.source.ConfigSource
import spray.http.{HttpCredentials, HttpEntity, HttpHeader, HttpHeaders, HttpRequest, HttpResponse, StatusCodes, Uri}
import spray.routing.{RequestContext, Route}

import scala.concurrent.blocking
import scala.language.postfixOps
import scala.util.control.NonFatal

/**
 * From https://github.com/bthuillier/spray/commit/d31fc1b5e1415e1b908fe7d1f01f364a727e2593 with extra bits from http://www.cakesolutions.net/teamblogs/http-proxy-with-spray .
 * Replace when Spray has its own version.
 *
 * @author david
 * @since 9/14/15
 */
trait HttpClientDirectives extends Loggable {
  implicit val system = ActorSystem("dashboardServer",ConfigSource.config)

  /**
    * Proxy the request to the specified base uri appended with the unmatched path.
    *
    */
  def forwardUnmatchedPath(baseUri: Uri,maybeCredentials:Option[HttpCredentials] = None): Route = {
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
  def requestWithUnmatchedPath(baseUri:Uri, route:(HttpResponse,Uri) => Route,maybeCredentials:Option[HttpCredentials] = None): Route = {
    ctx => {

      val resourceUri = baseUri.withPath(baseUri.path.++(ctx.unmatchedPath)).withQuery(ctx.request.uri.query)
      blocking {
        requestUriThenRoute(resourceUri,route,maybeCredentials)(ctx)
      }
    }
  }

  /**
    * Just pass the result through
    */
  def passThrough(httpResponse: HttpResponse,uri: Uri):Route = ctx => ctx.complete(httpResponse.entity.asString)

  /**
    * proxy the request to the specified uri with the unmatched path, then use the returned entity (as a string) to complete the route.
    *
    */
  def requestUriThenRoute(
                           resourceUri:Uri,
                          route:(HttpResponse,Uri) => Route = passThrough,
                          maybeCredentials:Option[HttpCredentials] = None
                         ): Route = {
    ctx => {
      blocking {
        val httpResponse = httpResponseForUri(resourceUri, ctx, maybeCredentials)
        info(s"Got $httpResponse for $resourceUri")

        handleCommonErrorsOrRoute(route)(httpResponse, resourceUri)(ctx)
      }
    }
  }

  private def httpResponseForUri(resourceUri:Uri,ctx: RequestContext,maybeCredentials:Option[HttpCredentials] = None):HttpResponse = {

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