package net.shrine.protocol.handlers

/**
 * @author clint
 * @date Oct 3, 2014
 */
trait ReadResultOutputTypesHandler[Req, Resp] {
  def readResultOutputTypes(request: Req): Resp
}
