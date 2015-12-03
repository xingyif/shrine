package net.shrine.protocol.handlers

/**
 * @author clint
 * @date Mar 27, 2014
 */
trait FlagQueryHandler[Req, Resp] {
  def flagQuery(request: Req, shouldBroadcast: Boolean = true): Resp
}