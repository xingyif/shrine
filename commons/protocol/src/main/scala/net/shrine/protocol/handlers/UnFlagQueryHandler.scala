package net.shrine.protocol.handlers

/**
 * @author clint
 * @date Apr 18, 2014
 */
trait UnFlagQueryHandler[Req, Resp] {
  def unFlagQuery(request: Req, shouldBroadcast: Boolean = true): Resp
}