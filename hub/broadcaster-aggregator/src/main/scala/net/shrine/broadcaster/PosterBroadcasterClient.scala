package net.shrine.broadcaster

import net.shrine.client.Poster
import net.shrine.protocol.BroadcastMessage
import scala.concurrent.Future
import scala.concurrent.blocking
import net.shrine.protocol.SingleNodeResult
import net.shrine.protocol.MultiplexedResults
import net.shrine.protocol.ResultOutputType
import scala.xml.XML

/**
 * @author clint
 * @date Mar 3, 2014
 */
final case class PosterBroadcasterClient(poster: Poster, breakdownTypes: Set[ResultOutputType]) extends BroadcasterClient {
  override def broadcast(message: BroadcastMessage): Future[Iterable[SingleNodeResult]] = {
    //TODO: REVISIT
    import scala.concurrent.ExecutionContext.Implicits.global
    
    for {
      response <- Future { blocking { poster.post(message.toXmlString) } }
    } yield {
      //TODO: Better handling of parsing failure; for now, this will complete the mapped
      //Future with an exception if parsing fails, which will hopefully have the original
      //parsing-failure exception as a root cause.
      MultiplexedResults.fromXml(breakdownTypes)(XML.loadString(response.body)).get.results
    }
  }
}