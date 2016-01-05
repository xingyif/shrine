package net.shrine.dashboard.httpclient

import java.io.InputStream

import net.shrine.log.Loggable
import spray.can.Http
import akka.io.IO
import akka.actor.{ActorRef, ActorSystem}
import spray.http.{HttpEntity, StatusCodes, HttpRequest, HttpResponse, Uri}
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
  def forwardUnmatchedPath(baseUri: Uri)(implicit system: ActorSystem): Route = {
    def completeWithEntityAsString(httpResponse:HttpResponse,uri:Uri):Route = {
      ctx => {
        ctx.complete(httpResponse.entity.asString)
      }
    }
    requestWithUnmatchedPath(baseUri,completeWithEntityAsString)
  }

  /**
    * Make the request to the specified base uri appended with the unmatched path, then use the returned entity (as a string) to complete the route.
    *
    */
  def requestWithUnmatchedPath(baseUri:Uri, route:(HttpResponse,Uri) => Route)(implicit system: ActorSystem): Route = {
    ctx => {
      val resourceUri = baseUri.withPath(baseUri.path.++(ctx.unmatchedPath))
      requestUriThenRoute(resourceUri,route)(system)(ctx)
    }
  }

  /**
    * proxy the request to the specified uri with the unmatched path, then use the returned entity (as a string) to complete the route.
    *
    */
  def requestUriThenRoute(resourceUri:Uri, route:(HttpResponse,Uri) => Route)(implicit system: ActorSystem): Route = {
    ctx => {
      val httpResponse = httpResponseForUri(resourceUri,ctx)(system)
      handleCommonErrorsOrRoute(route)(httpResponse,resourceUri)(ctx)
    }
  }

  private def httpResponseForUri(resourceUri:Uri,ctx: RequestContext)(implicit system: ActorSystem):HttpResponse = {

    if(resourceUri.scheme == "classpath") ClasspathResourceHttpClient.loadFromResource(resourceUri.path.toString())
    else HttpClient.webApiCall(HttpRequest(ctx.request.method,resourceUri))
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

  def webApiCall(request:HttpRequest)(implicit system: ActorSystem): HttpResponse = {
    val transport: ActorRef = IO(Http)(system)

    debug(s"Requesting $request")
    blocking {
      val future:Future[HttpResponse] = for {
        response <- transport.ask(request)(10 seconds).mapTo[HttpResponse]
      } yield response
      try {
        Await.result(future, 10 seconds)
      }
      catch {
        case x:TimeoutException => HttpResponse(status = StatusCodes.RequestTimeout,entity = HttpEntity(s"${request.uri} timed out after 10 seconds. ${x.getMessage}"))
          //todo is there a better message? What comes up in real life?
        case NonFatal(x) => {
          info(s"${request.uri} failed with ${x.getMessage}",x)
          HttpResponse(status = StatusCodes.InternalServerError,entity = HttpEntity(s"${request.uri} failed with ${x.getMessage}"))
        }
      }
    }
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