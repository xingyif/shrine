package net.shrine.protocol

/**
 * @author clint
 * @date Dec 4, 2012
 */
final case class AggregatedReadQueryResultResponse(
    override val queryId: Long, 
    override val results: Seq[QueryResult]) extends AbstractReadQueryResultResponse("aggregatedReadQueryResultResponse", queryId)

object AggregatedReadQueryResultResponse extends AbstractReadQueryResultResponse.Companion(new AggregatedReadQueryResultResponse(_, _) )