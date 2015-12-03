package net.shrine.aggregation

import net.shrine.protocol.DeleteQueryResponse

/**
 * @author Bill Simons
 * @date 8/12/11
 * @link http://cbmi.med.harvard.edu
 * @link http://chip.org
 *       <p/>
 *       NOTICE: This software comes with NO guarantees whatsoever and is
 *       licensed as Lgpl Open Source
 * @link http://www.gnu.org/licenses/lgpl.html
 */
final class DeleteQueryAggregator extends IgnoresErrorsAggregator[DeleteQueryResponse]