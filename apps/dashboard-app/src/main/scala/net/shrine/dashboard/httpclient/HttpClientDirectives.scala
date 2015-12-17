package net.shrine.dashboard.httpclient

import java.io.InputStream

import net.shrine.log.Loggable
import spray.can.Http
import akka.io.IO
import akka.actor.{ActorRef, ActorSystem}
import spray.http.{HttpRequest, Uri}
import spray.routing.Route
import akka.pattern.ask

import scala.concurrent.{Await, Future, blocking}

import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

import scala.concurrent.ExecutionContext.Implicits.global

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
    def completeWithString(string:String):Route = {
      ctx ⇒ {
        ctx.complete(string)
      }
    }
    requestWithUnmatchedPath(baseUri,completeWithString)
  }

  /**
    * Make the request to the specified base uri appended with the unmatched path, then use the returned entity (as a string) to complete the route.
    *
    */
  def requestWithUnmatchedPath(baseUri:Uri, route:String => Route)(implicit system: ActorSystem): Route = {
    ctx => {
      val resourceUri = baseUri.withPath(baseUri.path.++(ctx.unmatchedPath))

      val string =  if(resourceUri.scheme == "classpath") ResourceClient.loadFromResource(resourceUri.path.toString())
                    else HttpClient.webApiCall(HttpRequest(ctx.request.method,resourceUri))
      route(string)(ctx)
    }
  }

  /**
    * proxy the request to the specified uri with the unmatched path, then use the returned entity (as a string) to complete the route.
    *
    */
  def requestThenRoute(resourceUri:Uri, route:String => Route)(implicit system: ActorSystem): Route = {
    //todo start here. Refactor and mix with previous after you see it work.

    ctx => {
      val string =  if(resourceUri.scheme == "classpath") ResourceClient.loadFromResource(resourceUri.path.toString())
      else HttpClient.webApiCall(HttpRequest(ctx.request.method,resourceUri))
      route(string)(ctx)
    }
  }

}

object HttpClientDirectives extends HttpClientDirectives

object HttpClient extends Loggable {

  def webApiCall(request:HttpRequest)(implicit system: ActorSystem): String = {
    val transport: ActorRef = IO(Http)(system)

    debug(s"Requesting $request")
    blocking {
      val future:Future[String] = for {
        string <- transport.ask(request)(10 seconds).mapTo[String]
      } yield string
      Await.result(future,10 seconds)
    }
  }
}

object ResourceClient {

  def loadFromResource(resourceName:String):String = {
    blocking {
      val cleanResourceName = if (resourceName.startsWith ("/") ) resourceName.drop(1)
                              else resourceName
      val classLoader = getClass.getClassLoader
      val is: InputStream = classLoader.getResourceAsStream (cleanResourceName)
      scala.io.Source.fromInputStream (is).mkString
    }
  }
}