package net.shrine.broadcaster

import net.shrine.protocol.SingleNodeResult

import scala.concurrent.Future

/**
 * @author clint
 * @since Nov 15, 2013
 */
trait Multiplexer {
  def processResponse(response: SingleNodeResult): Unit
  
  def responses: Future[Iterable[SingleNodeResult]]
}