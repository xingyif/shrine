package net.shrine.broadcaster

import net.shrine.client.{HttpResponse, Poster}
import net.shrine.log.Loggable
import net.shrine.protocol.{NodeId, BroadcastMessage, SingleNodeResult, MultiplexedResults, ResultOutputType}
import scala.concurrent.Future
import scala.concurrent.blocking
import scala.util.{Success, Try}
import scala.xml.XML

/**
 * @author clint
 * @since Mar 3, 2014
 */
final case class PosterBroadcasterClient(poster: Poster, breakdownTypes: Set[ResultOutputType]) extends BroadcasterClient with Loggable {
  override def broadcast(message: BroadcastMessage): Future[Iterable[SingleNodeResult]] = {
    //TODO: REVISIT
    import scala.concurrent.ExecutionContext.Implicits.global
    
    for {
      response: HttpResponse <- Future { blocking { poster.post(message.toXmlString) } }
    } yield {

      val tryResults: Try[Seq[SingleNodeResult]] = MultiplexedResults.fromXml(breakdownTypes)(XML.loadString(response.body)).map(_.results)

      //todo use fold()() in Scala 2.12
      tryResults match {
        case Success(results) => results
        case scala.util.Failure(ex) => {
          error(s"Exception while parsing response with status ${response.statusCode} from ${poster.url} while parsing ${response.body}",ex)
          //todo where to get a real nodeId?
          val x = CouldNotParseResultsException(response.statusCode,poster.url,response.body,ex)
          x.fillInStackTrace()
          Seq(net.shrine.protocol.Failure(NodeId(poster.url),x))
        }
      }
    }
  }
}

case class CouldNotParseResultsException(statusCode:Int,url:String,body:String,cause:Throwable) extends Exception(CouldNotParseResultsException.createMessage(statusCode,url),cause)

object CouldNotParseResultsException {
  def createMessage(statusCode:Int,url:String) = s"While parsing response with status ${statusCode} from ${url}"
}