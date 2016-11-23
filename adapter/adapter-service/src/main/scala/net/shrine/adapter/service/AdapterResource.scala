package net.shrine.adapter.service

import net.shrine.log.Loggable

import scala.util.Try
import scala.util.control.NonFatal
import javax.ws.rs.{GET, POST, Path, Produces}
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.Response.ResponseBuilder

import net.shrine.protocol.{BroadcastMessage, ResultOutputType, ResultOutputTypes}

import scala.xml.XML
import net.shrine.util.StringEnrichments

/**
 * @author clint
 * @since Nov 15, 2013
 */
@Path("adapter")
@Produces(Array(MediaType.APPLICATION_XML))
//NB: Is a case class to get apply() on the companion object for smoother testing
final case class AdapterResource(service: AdapterRequestHandler) extends Loggable {
  @POST
  @Path("requests")
  def handleRequest(messageXml: String): Response = {
    import AdapterResource.StatusCodes._
    
    def handleRequest(message: BroadcastMessage): Try[ResponseBuilder] = Try {
      info(s"Running request ${message.requestId} from user: ${message.networkAuthn.domain}:${message.networkAuthn.username} of type ${message.request.requestType.toString}")

      val adapterResult = service.handleRequest(message)

      val responseString = adapterResult.toXmlString

      Response.ok.entity(responseString)
    }.recover {
      case NonFatal(e) =>
        error("Error processing request: ", e)
        throw e
    }

    def handleParseError(e: Throwable): Try[ResponseBuilder] = {
      debug(s"Failed to unmarshall broadcast message XML: '$messageXml'")

      error("Couldn't understand request: ", e)

      scala.util.Failure(e)
    }

    import StringEnrichments._
    
    val broadcastMessageAttempt = messageXml.tryToXml.flatMap(BroadcastMessage.fromXml)
    
    val builderAttempt = broadcastMessageAttempt.transform(handleRequest, handleParseError)
    
    builderAttempt.get.build()
  }

  @GET
  @Path("requests")
  def handleGet: Response = {
    Response.ok.entity("THIS IS A TEST THIS IS A TEST EHLLO").build()
  }
}

//NB: extends Handler => Resource for smoother testing
object AdapterResource extends (AdapterRequestHandler => AdapterResource) {
  object StatusCodes {
    val InternalError = 500
    val BadRequest = 400
  }
}