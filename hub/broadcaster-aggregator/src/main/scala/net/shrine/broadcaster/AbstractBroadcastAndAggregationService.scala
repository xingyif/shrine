package net.shrine.broadcaster

import net.shrine.protocol.BroadcastMessage
import net.shrine.aggregation.Aggregator
import scala.concurrent.Future
import net.shrine.protocol.BaseShrineResponse

/**
 * @author clint
 * @date Feb 28, 2014
 */
abstract class AbstractBroadcastAndAggregationService(broadcasterClient: BroadcasterClient, processMessage: BroadcastMessage => BroadcastMessage = identity) extends BroadcastAndAggregationService {
  override def sendAndAggregate(message: BroadcastMessage, aggregator: Aggregator, shouldBroadcast: Boolean): Future[BaseShrineResponse] = {

    val futureResponses = broadcasterClient.broadcast(processMessage(message))

    import scala.concurrent.ExecutionContext.Implicits.global
    
    for {
      responses <- futureResponses
    } yield {
      aggregator.aggregate(responses.filter(_ != null), Nil)
    }
  }
}