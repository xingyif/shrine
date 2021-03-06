package net.shrine.adapter.client

import java.net.{SocketTimeoutException, URL}

import org.xml.sax.SAXParseException

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.blocking
import scala.concurrent.duration.Duration
import scala.concurrent.duration.DurationInt
import scala.util.control.NonFatal
import scala.xml.{NodeSeq, XML}
import com.sun.jersey.api.client.ClientHandlerException
import net.shrine.client.{HttpResponse, Poster, TimeoutException}
import net.shrine.problem.{AbstractProblem, ProblemNotYetEncoded, ProblemSources}
import net.shrine.protocol.{BroadcastMessage, ErrorResponse, NodeId, Result, ResultOutputType, RootTagNotFoundException}

import scala.util.{Failure, Success, Try}
import scala.collection.immutable.Stream.Empty

/**
 * @author clint
 * @since Nov 15, 2013
 * 
 * 
 */
final class RemoteAdapterClient private (nodeId:NodeId,val poster: Poster, val breakdownTypes: Set[ResultOutputType]) extends AdapterClient {
  import RemoteAdapterClient._
  
  //NB: Overriding apply in the companion object screws up case-class code generation for some reason, so
  //we add the would-have-been-generated methods here 
  override def toString = s"RemoteAdapterClient($poster)"
  
  override def hashCode: Int = 31 * (if(poster == null) 1 else poster.hashCode)
  
  override def equals(other: Any): Boolean = other match {
    case that: RemoteAdapterClient if that != null => poster == that.poster
    case _ => false
  }

  def url:Option[URL] = Some(new URL(poster.url))

  //TODO: Revisit this
  import scala.concurrent.ExecutionContext.Implicits.global

  override def query(request: BroadcastMessage): Future[Result] = {
    val requestXml = request.toXml

    Future {
      blocking {

        val response: HttpResponse = poster.post(requestXml.toString())
        interpretResponse(response)
      }
    }.recover {
      case e if isTimeout(e) => throw new TimeoutException(s"Invoking adapter at ${poster.url} timed out", e)
    }
  }

  def interpretResponse(response:HttpResponse):Result = {
    if(response.statusCode <= 400){
      val responseXml = response.body

      import scala.concurrent.duration._

      //Should we know the NodeID here?  It would let us make a better error response.
      Try(XML.loadString(responseXml)).flatMap(Result.fromXml(breakdownTypes)) match {
        case Success(result) => result
        case Failure(x) => {
          val errorResponse = x match {
            case sx: SAXParseException => ErrorResponse(CouldNotParseXmlFromAdapter(poster.url,response.statusCode,responseXml,sx))
            case rtnfx: RootTagNotFoundException => ErrorResponse(CouldNotParseXmlFromAdapter(poster.url,response.statusCode,responseXml,rtnfx))
            case _ => ErrorResponse(ProblemNotYetEncoded(s"Couldn't understand response from adapter at '${poster.url}': $responseXml", x))
          }
          Result(nodeId, 0.milliseconds, errorResponse)
        }
      }
    }
    else {
      Result(nodeId,0.milliseconds,ErrorResponse(HttpErrorCodeFromAdapter(poster.url,response.statusCode,response.body)))
    }
  }
}

object RemoteAdapterClient {
  
  def apply(nodeId:NodeId,poster: Poster, breakdownTypes: Set[ResultOutputType]): RemoteAdapterClient = {
    //NB: Replicate URL-munging that used to be performed by JerseyAdapterClient
    val posterToUse = {
      if(poster.url.endsWith("requests")) { poster }
      else { poster.mapUrl(_ + "/requests") }
    }
    
    new RemoteAdapterClient(nodeId,posterToUse, breakdownTypes)
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

case class HttpErrorCodeFromAdapter(url:String,statusCode:Int,responseBody:String) extends AbstractProblem(ProblemSources.Adapter) {
  override def summary: String = "Hub received a fatal error response"

  override def description: String = s"Hub received error code $statusCode from the adapter at $url"

  override def detailsXml:NodeSeq = {
    if (responseBody.isEmpty)
      <details>"Error response contained no body"</details>
    else {
      val result = s"Http response body was $responseBody"
      <details>{result}</details>
    }
  }
}

case class CouldNotParseXmlFromAdapter(url:String,statusCode:Int,responseBody:String,x: Exception) extends AbstractProblem(ProblemSources.Adapter) {

  override def throwable = Some(x)

  override def summary: String = s"Hub could not parse response from adapter"

  override def description: String = s"Hub could not parse xml from $url due to ${x.toString}"

  override def detailsXml:NodeSeq = <details>
    {s"Http response code was $statusCode and the body was $responseBody"}
    {throwableDetail}
  </details>
}