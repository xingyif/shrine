package net.shrine.broadcaster

import net.shrine.protocol.{SingleNodeResult, BroadcastMessage, BaseShrineResponse}
import net.shrine.aggregation.Aggregator
import scala.concurrent.Future

/**
 * @author clint
 * @since Feb 28, 2014
 */
abstract class AbstractBroadcastAndAggregationService(broadcasterClient: BroadcasterClient, processMessage: BroadcastMessage => BroadcastMessage = identity) extends BroadcastAndAggregationService {
  override def sendAndAggregate(message: BroadcastMessage, aggregator: Aggregator, shouldBroadcast: Boolean): Future[BaseShrineResponse] = {

    val futureResponses: Future[Iterable[SingleNodeResult]] = broadcasterClient.broadcast(processMessage(message))

    import scala.concurrent.ExecutionContext.Implicits.global
    
    for {
      responses <- futureResponses
    } yield {
      aggregator.aggregate(responses.filter(_ != null), Nil,message)
    }
  }
}