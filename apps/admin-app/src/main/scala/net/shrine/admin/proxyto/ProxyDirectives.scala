package net.shrine.admin.proxyto

import net.shrine.log.Loggable
import spray.can.Http
import akka.io.IO
import akka.actor.{ActorRef, ActorSystem}
import spray.http.{HttpResponse, HttpHeader, HttpHeaders, HttpRequest, Uri}
import spray.routing.{Route, RequestContext}
import akka.pattern.ask

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.{Duration, FiniteDuration, DurationInt}

/**
 * From https://github.com/bthuillier/spray/commit/d31fc1b5e1415e1b908fe7d1f01f364a727e2593 with extra bits from http://www.cakesolutions.net/teamblogs/http-proxy-with-spray .
 * Replace when Spray has its own proxy if this class is even still in use then.
 *
 * @author david
 * @since 9/14/15
 */
trait ProxyDirectives extends Loggable {

  private def sending(f: RequestContext ⇒ HttpRequest)(implicit system: ActorSystem): Route = {
    val transport: ActorRef = IO(Http)(system)
    ctx ⇒ {
      val request = f(ctx)
      debug(s"Forwarding request to happy service $request")
//todo how to tell it to do another function next? Read Akka docs      transport.tell(request, ctx.responder)
      val responseFuture: Future[Any] = transport.ask(request)(10 seconds)

      val responseFromTunnel:Any = Await.ready(responseFuture,10 seconds)
      responseFromTunnel match {
        case httpResponse:HttpResponse => {
          ctx.complete(httpResponse.entity)
        }
        case x => {
          throw new IllegalStateException(s"Got $x instead of an HttpResponse")
        }
      }
    }
  }

  private def stripHostHeader(headers: List[HttpHeader] = Nil) =
    headers filterNot (header => header is (HttpHeaders.Host.lowercaseName))

  /**
   * proxy the request to the specified uri
   *
   */
  def proxyTo(uri: Uri)(implicit system: ActorSystem): Route = {
    sending(ctx =>
      ctx.request.copy(
        uri = uri,
        headers = stripHostHeader(ctx.request.headers)
      )
    )
  }

  /**
   * proxy the request to the specified uri with the unmatched path
   *
   */
  def proxyToUnmatchedPath(uri: Uri)(implicit system: ActorSystem): Route = {
    sending(ctx ⇒
      ctx.request.copy(
        uri = uri.withPath(uri.path.++(ctx.unmatchedPath)),
        headers = stripHostHeader(ctx.request.headers)
      )
    )
  }
}

object ProxyDirectives extends ProxyDirectives