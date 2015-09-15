package net.shrine.admin.proxyto

import spray.can.Http
import akka.io.IO
import akka.actor.ActorSystem
import spray.http.{ HttpRequest, Uri }
import spray.routing.{Route, RequestContext}

/**
 * From https://github.com/bthuillier/spray/commit/d31fc1b5e1415e1b908fe7d1f01f364a727e2593 . Replace when Spray has
 * its own proxy if this class is even still in use.
 *
 * @author david
 * @since 9/14/15
 */
trait ProxyDirectives {

  private def sending(f: RequestContext ⇒ HttpRequest)(implicit system: ActorSystem): Route = {
    val transport = IO(Http)(system)
    ctx ⇒ transport.tell(f(ctx), ctx.responder)
  }

  /**
   * proxy the request to the specified uri
   *
   */
  def proxyTo(uri: Uri)(implicit system: ActorSystem): Route = {
    sending(_.request.copy(uri = uri))
  }

  /**
   * proxy the request to the specified uri with the unmatched path
   *
   */
  def proxyToUnmatchedPath(uri: Uri)(implicit system: ActorSystem): Route = {
    sending(ctx ⇒ ctx.request.copy(uri = uri.withPath(uri.path.++(ctx.unmatchedPath))))
  }
}

object ProxyDirectives extends ProxyDirectives