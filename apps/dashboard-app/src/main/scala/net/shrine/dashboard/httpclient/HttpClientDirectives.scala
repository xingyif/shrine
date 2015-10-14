package net.shrine.dashboard.httpclient

import net.shrine.log.Loggable
import spray.can.Http
import akka.io.IO
import akka.actor.{ActorRef, ActorSystem}
import spray.http.{HttpResponse, HttpRequest, Uri}
import spray.routing.{Route, RequestContext}
import akka.pattern.ask

import scala.concurrent.duration.DurationInt
import scala.util.Try

import scala.concurrent.ExecutionContext.Implicits.global

/**
 * From https://github.com/bthuillier/spray/commit/d31fc1b5e1415e1b908fe7d1f01f364a727e2593 with extra bits from http://www.cakesolutions.net/teamblogs/http-proxy-with-spray .
 * Replace when Spray has its own version.
 *
 * @author david
 * @since 9/14/15
 */
trait HttpClientDirectives extends Loggable {

  private def sending(f: RequestContext ⇒ HttpRequest)(implicit system: ActorSystem): Route = {
    val transport: ActorRef = IO(Http)(system)
    ctx ⇒ {
      val request = f(ctx)
      debug(s"Forwarding request to happy service $request")
      import scala.concurrent.blocking
      blocking {
        transport.ask(request)(10 seconds).onComplete { tryAny: Try[Any] =>
//todo replace with fold when try gets it in scala 2.12
          val any = tryAny.get
          any match {
            case response: HttpResponse => ctx.complete(response.entity)
          }
        }
      }
    }
  }

  /**
   * proxy the request to the specified uri
   *
   */
  def httpRequest(uri: Uri)(implicit system: ActorSystem): Route = {
    sending { ctx =>
      HttpRequest(ctx.request.method,
        uri.withPath(uri.path.++(ctx.unmatchedPath)))
    }
  }

  /**
   * proxy the request to the specified uri with the unmatched path
   *
   */
  def httpRequestWithUnmatchedPath(uri: Uri)(implicit system: ActorSystem): Route = {
    sending { ctx ⇒
      HttpRequest(ctx.request.method,
                  uri.withPath(uri.path.++(ctx.unmatchedPath)))
    }
  }
}

object HttpClientDirectives extends HttpClientDirectives