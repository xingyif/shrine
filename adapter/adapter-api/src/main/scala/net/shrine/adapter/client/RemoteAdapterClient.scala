package net.shrine.adapter.client

import java.net.SocketTimeoutException
import net.shrine.problem.{ProblemNotInCodec, ProblemSources, AbstractProblem}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.blocking
import scala.concurrent.duration.Duration
import scala.concurrent.duration.DurationInt
import scala.util.control.NonFatal
import scala.xml.XML
import com.sun.jersey.api.client.ClientHandlerException
import net.shrine.client.TimeoutException
import net.shrine.protocol.BroadcastMessage
import net.shrine.protocol.ErrorResponse
import net.shrine.protocol.NodeId
import net.shrine.protocol.Result
import net.shrine.client.Poster
import scala.util.{Failure, Success, Try}
import net.shrine.protocol.ResultOutputType

/**
 * @author clint
 * @since Nov 15, 2013
 * 
 * 
 */
final class RemoteAdapterClient private (val poster: Poster, val breakdownTypes: Set[ResultOutputType]) extends AdapterClient {
  import RemoteAdapterClient._
  
  //NB: Overriding apply in the companion object screws up case-class code generation for some reason, so
  //we add the would-have-been-generated methods here 
  override def toString = s"RemoteAdapterClient($poster)"
  
  override def hashCode: Int = 31 * (if(poster == null) 1 else poster.hashCode)
  
  override def equals(other: Any): Boolean = other match {
    case that: RemoteAdapterClient if that != null => poster == that.poster
    case _ => false
  }
  
  //TODO: Revisit this
  import scala.concurrent.ExecutionContext.Implicits.global

  override def query(request: BroadcastMessage): Future[Result] = {
    val requestXml = request.toXml

    Future {
      blocking {

        val response = poster.post(requestXml.toString())

        val responseXml = response.body

        import scala.concurrent.duration._

        //Should we know the NodeID here?  It would let us make a better error response.
        Try(XML.loadString(responseXml)).flatMap(Result.fromXml(breakdownTypes)) match {
          case Success(result) => result
          case Failure(x) => {
            val errorResponse = x match {
              case _ => ErrorResponse(ProblemNotInCodec(s"Couldn't understand response from adapter at '${poster.url}': $responseXml", x))
            }
            Result(NodeId.Unknown, 0.milliseconds, errorResponse)
          }
        }
      }
    }.recover {
      case e if isTimeout(e) => throw new TimeoutException(s"Invoking adapter at ${poster.url} timed out", e)
    }
  }
}

object RemoteAdapterClient {
  
  def apply(poster: Poster, breakdownTypes: Set[ResultOutputType]): RemoteAdapterClient = {
    //NB: Replicate URL-munging that used to be performed by JerseyAdapterClient
    val posterToUse = {
      if(poster.url.endsWith("requests")) { poster }
      else { poster.mapUrl(_ + "/requests") }
    }
    
    new RemoteAdapterClient(posterToUse, breakdownTypes)
  }
  
  def isTimeout(e: Throwable): Boolean = e match {
    case e: SocketTimeoutException => true
    case e: ClientHandlerException => {
      val cause = e.getCause

      cause != null && cause.isInstanceOf[SocketTimeoutException]
    }
    case _ => false
  }
}
