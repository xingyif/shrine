package net.shrine.aggregation

import net.shrine.protocol.SingleNodeResult
import net.shrine.protocol.ErrorResponse
import net.shrine.protocol.ShrineResponse
import net.shrine.protocol.BaseShrineResponse

/**
 * @author Bill Simons
 * @date 5/23/11
 * @link http://cbmi.med.harvard.edu
 * @link http://chip.org
 *       <p/>
 *       NOTICE: This software comes with NO guarantees whatsoever and is
 *       licensed as Lgpl Open Source
 * @link http://www.gnu.org/licenses/lgpl.html
 */
trait Aggregator {
  
  def aggregate(results: Iterable[SingleNodeResult], errors: Iterable[ErrorResponse]): BaseShrineResponse
}