package net.shrine.broadcaster

import net.shrine.protocol.ShrineRequest
import scala.concurrent.Future
import net.shrine.protocol.ShrineResponse
import net.shrine.aggregation.Aggregator
import net.shrine.protocol.BroadcastMessage
import net.shrine.protocol.RunQueryRequest
import net.shrine.aggregation.RunQueryAggregator
import net.shrine.aggregation.ReadQueryResultAggregator
import net.shrine.protocol.BaseShrineResponse
import net.shrine.protocol.BaseShrineRequest
import net.shrine.protocol.AuthenticationInfo

/**
 * @author clint
 * @date Mar 13, 2013
 */
trait BroadcastAndAggregationService {
  def sendAndAggregate(message: BroadcastMessage, aggregator: Aggregator, shouldBroadcast: Boolean): Future[BaseShrineResponse]
  
  def sendAndAggregate(networkAuthn: AuthenticationInfo, request: BaseShrineRequest, aggregator: Aggregator, shouldBroadcast: Boolean): Future[BaseShrineResponse] = {
    val (queryId, requestToSend) = addQueryId(request)
    
    val broadcastMessage = BroadcastMessage(queryId.getOrElse(newQueryId), networkAuthn, requestToSend)
    
    val aggregatorWithCorrectQueryId = addQueryId(broadcastMessage, aggregator)
    
    sendAndAggregate(broadcastMessage, aggregatorWithCorrectQueryId, shouldBroadcast)
  }
  
  protected[broadcaster] def addQueryId(request: BaseShrineRequest): (Option[Long], BaseShrineRequest) = request match {
    case runQueryReq: RunQueryRequest => {
      val queryId = newQueryId

      (Some(queryId), runQueryReq.withNetworkQueryId(queryId))
    }
    case _ => (None, request)
  }
  
  protected[broadcaster] def addQueryId(message: BroadcastMessage, aggregator: Aggregator): Aggregator = aggregator match {
    case runQueryAggregator: RunQueryAggregator => runQueryAggregator.withQueryId(message.requestId)
    case readQueryResultAggregator: ReadQueryResultAggregator => readQueryResultAggregator.withShrineNetworkQueryId(message.requestId) 
    case _ => aggregator
  }
  
  private def newQueryId = BroadcastMessage.Ids.next
}