package net.shrine.aggregation

import net.shrine.protocol.RunQueryRequest

/**
 * @author clint
 * @date Mar 14, 2013
 */
object Aggregators {
  def forRunQueryRequest(addAggregatedResult: Boolean)(request: RunQueryRequest): RunQueryAggregator = {
    new RunQueryAggregator(
        //NB: Use dummy queryId, since this will be assigned by the BroadcasterService if needed
        //TODO: Don't use dummy values
        -1L,
        request.authn.username,
        //TODO: Confusing field name: this is called groupId, but we're passing the projectId
        request.projectId, 
        request.queryDefinition,
        addAggregatedResult)
  }
}