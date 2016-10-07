package net.shrine.aggregation

import net.shrine.protocol.{BaseShrineResponse, BroadcastMessage, ErrorResponse, ShrineResponse, SingleNodeResult}

/**
 * @author Bill Simons
 * @since 5/23/11
 * @see http://cbmi.med.harvard.edu
 * @see http://chip.org
 *       <p/>
 *       NOTICE: This software comes with NO guarantees whatsoever and is
 *       licensed as Lgpl Open Source
 * @see http://www.gnu.org/licenses/lgpl.html
 */
trait Aggregator {
  
  def aggregate(results: Iterable[SingleNodeResult], errors: Iterable[ErrorResponse], respondingTo: BroadcastMessage ): BaseShrineResponse
}