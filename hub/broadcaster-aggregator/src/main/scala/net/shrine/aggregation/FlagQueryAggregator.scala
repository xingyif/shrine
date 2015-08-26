package net.shrine.aggregation

import net.shrine.protocol.FlagQueryResponse

/**
 * @author clint
 * @date Mar 27, 2014
 */
//TODO: Perhaps extends PackagesErrorsAggregator?
final class FlagQueryAggregator extends IgnoresErrorsAggregator[FlagQueryResponse]